package net.sparktank.nemain.model;

public class NemainEvent {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	NemainEvent (String entryText, int entryYear, int entryMonth, int entryDay) {
		this.entryText = entryText;
		this.entryYear = entryYear;
		this.entryMonth = entryMonth;
		this.entryDay = entryDay;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Properties.
	
	private String entryText;
	private int entryYear;
	private int entryMonth;
	private int entryDay;
	
	public String getEntryText() {
		return entryText;
	}
	public void setEntryText(String entryText) {
		this.entryText = entryText;
	}
	
	public int getEntryYear() {
		return entryYear;
	}
	public void setEntryYear(int entryYear) {
		this.entryYear = entryYear;
	}
	
	public int getEntryMonth() {
		return entryMonth;
	}
	public void setEntryMonth(int entryMonth) {
		this.entryMonth = entryMonth;
	}
	
	public int getEntryDay() {
		return entryDay;
	}
	public void setEntryDay(int entryDay) {
		this.entryDay = entryDay;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString() {
		return entryText;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
