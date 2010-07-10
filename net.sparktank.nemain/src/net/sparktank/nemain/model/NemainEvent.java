package net.sparktank.nemain.model;

import net.sparktank.nemain.helpers.EqualHelper;


public class NemainEvent extends NemainDate {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public NemainEvent (String entryText, int entryYear, int entryMonth, int entryDay) {
		super(entryYear, entryMonth, entryDay);
		this.entryText = entryText;
	}
	
	public NemainEvent (String entryText, NemainDate date) {
		super(date);
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
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.toString());
		sb.append(' ');
		sb.append(getEntryText());
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object aThat) {
		if ( aThat == null ) return false;
		if ( this == aThat ) return true;
		if ( !(aThat instanceof NemainEvent) ) return false;
		NemainEvent that = (NemainEvent)aThat;
		
		return super.equals(aThat)
			&& EqualHelper.areEqual(entryText, that.getEntryText());
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + super.hashCode();
		hash = hash * 31 + (entryText == null ? 0 : entryText.hashCode());
		return hash;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
