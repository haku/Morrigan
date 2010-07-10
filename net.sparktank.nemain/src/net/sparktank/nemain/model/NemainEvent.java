package net.sparktank.nemain.model;


public class NemainEvent extends NemainDate {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public NemainEvent (String entryText, int entryYear, int entryMonth, int entryDay) {
		super(entryYear, entryMonth, entryDay);
		this.entryText = entryText;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Properties.
	
	private String entryText;
	
	public String getEntryText() {
		return entryText;
	}
	
	@Override
	public boolean isMutable() {
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public NemainDate getDate () {
		return super.getThis();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.toString());
		sb.append(' ');
		sb.append(getEntryText());
		
		return sb.toString();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
