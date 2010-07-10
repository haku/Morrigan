package net.sparktank.nemain.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class NemainEvent {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	NemainEvent (String entryText, int entryYear, int entryMonth, int entryDay) {
		this.entryText = entryText;
		this.entryDate = new GregorianCalendar(entryYear, entryMonth - 1, entryDay).getTime();
	}
	
	NemainEvent (String entryText, Date date) {
		this.entryText = entryText;
		this.entryDate = date;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Properties.
	
	private String entryText;
	private Date entryDate;
	
	public String getEntryText() {
		return entryText;
	}
	public void setEntryText(String entryText) {
		this.entryText = entryText;
	}
	
	public Date getEntryDate() {
		return entryDate;
	}
	public void setEntryDate(Date entryDate) {
		this.entryDate = entryDate;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append('(');
		sb.append(FORMATTER.format(getEntryDate()));
		sb.append(") ");
		sb.append(getEntryText());
		
		return sb.toString();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
