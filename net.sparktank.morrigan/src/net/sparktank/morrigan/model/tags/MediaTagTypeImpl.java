package net.sparktank.morrigan.model.tags;

import net.sparktank.morrigan.model.media.MediaTagType;

public enum MediaTagTypeImpl implements MediaTagType {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	MANUAL(0),
	AUTOMATIC(1);
	
	String [] shortNames = {"M", "A"};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static MediaTagTypeImpl getFromIndex (int index) {
		switch (index) {
			case 0: return MANUAL;
			case 1: return AUTOMATIC;
			default: throw new IllegalArgumentException();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final int index;
	
	private MediaTagTypeImpl (int index) {
		this.index = index;
	}
	
	@Override
	public int getIndex () {
		return this.index;
	}
	
	@Override
	public String getShortName () {
		return this.shortNames[this.index];
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
