package com.vaguehope.morrigan.model.media;

public enum MediaTagType {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	MANUAL(0),
	AUTOMATIC(1);
	
	String [] shortNames = {"M", "A"};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static MediaTagType getFromIndex (int index) {
		switch (index) {
			case 0: return MANUAL;
			case 1: return AUTOMATIC;
			default: throw new IllegalArgumentException();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final int index;
	
	private MediaTagType (int index) {
		this.index = index;
	}
	
	public int getIndex () {
		return this.index;
	}
	
	public String getShortName () {
		return this.shortNames[this.index];
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
