package com.vaguehope.morrigan.model.media;

public enum FileExistance {

	/**
	 * No DB entry.
	 */
	UNKNOWN(false),

	/**
	 * DB entry, not marked as missing.
	 */
	EXISTS(true),

	/**
	 * DB entry, marked as missing.
	 */
	MISSING(true);

	private final boolean known;

	private FileExistance (final boolean known) {
		this.known = known;
	}

	public boolean isKnown () {
		return this.known;
	}

}
