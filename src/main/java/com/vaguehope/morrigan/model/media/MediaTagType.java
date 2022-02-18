package com.vaguehope.morrigan.model.media;

public enum MediaTagType {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	MANUAL(0, "M"),
	AUTOMATIC(1, "A");

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
	private final String shortName;

	private MediaTagType (int index, String shortName) {
		this.index = index;
		this.shortName = shortName;
	}

	public int getIndex () {
		return this.index;
	}

	public String getShortName () {
		return this.shortName;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
