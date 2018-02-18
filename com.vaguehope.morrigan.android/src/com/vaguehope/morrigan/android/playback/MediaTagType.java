package com.vaguehope.morrigan.android.playback;

public enum MediaTagType {

	MANUAL(0, "M"),
	AUTOMATIC(1, "A");

	private final int number;
	private final String shortName;

	private MediaTagType (final int number, final String shortName) {
		this.number = number;
		this.shortName = shortName;
	}

	public int getNumber () {
		return this.number;
	}

	public String getShortName () {
		return this.shortName;
	}

	public static MediaTagType getFromNumber (final int number) {
		switch (number) {
			case 0:
				return MANUAL;
			case 1:
				return AUTOMATIC;
			default:
				throw new IllegalArgumentException();
		}
	}

}
