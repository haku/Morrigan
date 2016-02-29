package com.vaguehope.morrigan.player;

public enum PlayItemType {

	PLAYABLE(false),
	STOP(true);

	private final boolean isPseudo;

	private PlayItemType (final boolean isPseudo) {
		this.isPseudo = isPseudo;
	}

	public boolean isPseudo () {
		return this.isPseudo;
	}

}
