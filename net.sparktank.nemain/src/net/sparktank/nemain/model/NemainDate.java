package net.sparktank.nemain.model;

import java.util.Calendar;

public class NemainDate {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private int entryYear;
	private int entryMonth;
	private int entryDay;
	
	public NemainDate () {
		Calendar cal = Calendar.getInstance();
		setFromCalendar(cal);
	}
	
	public NemainDate (int entryYear, int entryMonth, int entryDay) {
		this.entryYear = entryYear;
		this.entryMonth = entryMonth;
		this.entryDay = entryDay;
	}
	
	public NemainDate (NemainDate date) {
		this.entryYear = date.getYear();
		this.entryMonth = date.getMonth();
		this.entryDay = date.getDay();
	}
	
	public NemainDate (Calendar cal) {
		setFromCalendar(cal);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public int getYear() {
		return entryYear;
	}
	
	public int getMonth() {
		return entryMonth;
	}
	
	public int getDay() {
		return entryDay;
	}
	
	public boolean isMutable () {
		return true;
	}
	
	public void setYear(int entryYear) {
		if (!isMutable()) throw new IllegalArgumentException("Is immutable.");
		this.entryYear = entryYear;
	}
	
	public void setMonth(int entryMonth) {
		if (!isMutable()) throw new IllegalArgumentException("Is immutable.");
		this.entryMonth = entryMonth;
	}
	
	public void setDay(int entryDay) {
		if (!isMutable()) throw new IllegalArgumentException("Is immutable.");
		this.entryDay = entryDay;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Helper methods.
	
	public boolean isOnOrAfter (NemainDate date) {
		return isDateOnOrAfter(this, date);
	}
	
	public boolean isOnOrAfter (int ref_year, int ref_month, int ref_day) {
		return isDateOnOrAfter(this, ref_year, ref_month, ref_day);
	}
	
	public boolean isWithinNDaysAfter (NemainDate ref_date, int nDaysAfter) {
		return isWithinNDaysAfter(ref_date.getYear(), ref_date.getMonth(), ref_date.getDay(), nDaysAfter);
	}
	
	public boolean isWithinNDaysAfter (int ref_year, int ref_month, int ref_day, int nDaysAfter) {
		if (getYear() == 0) {
			return isDateOnOrAfter(ref_year, getMonth(), getDay(), ref_year, ref_month, ref_day)
					&& !isDateOnOrAfter(ref_year, getMonth(), getDay(), daysAfterDate(ref_year, ref_month, ref_day, nDaysAfter+1));
		} else {
			return isDateOnOrAfter(getYear(), getMonth(), getDay(), ref_year, ref_month, ref_day)
					&& !isDateOnOrAfter(getYear(), getMonth(), getDay(), daysAfterDate(ref_year, ref_month, ref_day, nDaysAfter+1));
		}
	}
	
	public NemainDate daysAfter (int nDays) {
		return daysAfterDate(this, nDays);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void setFromCalendar (Calendar cal) {
		this.entryYear = cal.get(Calendar.YEAR);
		this.entryMonth = cal.get(Calendar.MONTH) + 1;
		this.entryDay = cal.get(Calendar.DAY_OF_MONTH);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(getYear());
		sb.append('-');
		sb.append(getMonth());
		sb.append('-');
		sb.append(getDay());
		
		return sb.toString();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public NemainDate daysAfterDate (NemainDate ref_date, int nDays) {
		return daysAfterDate(ref_date.getYear(), ref_date.getMonth(), ref_date.getDay(), nDays);
	}
	
	static public NemainDate daysAfterDate (int ref_year, int ref_month, int ref_day, int nDays) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(ref_year, ref_month - 1, ref_day);
		cal.add(Calendar.DATE, nDays);
		return new NemainDate(cal);
	}
	
//	- - -
	
	static public boolean isDateOnOrAfter (NemainDate query_date, NemainDate ref_date) {
		return isDateOnOrAfter(query_date.getYear(), query_date.getMonth(), query_date.getDay(),
				ref_date.getYear(), ref_date.getMonth(), ref_date.getDay());
	}
	
	static public boolean isDateOnOrAfter (NemainDate query_date, int ref_year, int ref_month, int ref_day) {
		return isDateOnOrAfter(query_date.getYear(), query_date.getMonth(), query_date.getDay(),
				ref_year, ref_month, ref_day);
	}
	
	static public boolean isDateOnOrAfter (int query_year, int query_month, int query_day, NemainDate ref_date) {
		return isDateOnOrAfter(query_year, query_month, query_day,
				ref_date.getYear(), ref_date.getMonth(), ref_date.getDay());
	}
	
	static public boolean isDateOnOrAfter (int query_year, int query_month, int query_day, int ref_year, int ref_month, int ref_day) {
		if (query_year == 0) return true; // Is an annual event, so always in the future.
		
		if (query_year > ref_year) return true;
		if (query_year == ref_year && query_month > ref_month) return true;
		if (query_year == ref_year && query_month == ref_month && query_day >= ref_day) return true;
		
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
