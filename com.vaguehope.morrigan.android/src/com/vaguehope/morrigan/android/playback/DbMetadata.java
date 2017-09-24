package com.vaguehope.morrigan.android.playback;

public class DbMetadata {

	private final long id;
	private final String name;

	public DbMetadata (final long id, final String name) {
		this.id = id;
		this.name = name;
	}

	public long getId () {
		return this.id;
	}

	public String getName () {
		return this.name;
	}

}
