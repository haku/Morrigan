package com.vaguehope.morrigan.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

	private static final Map<String, PlayItemType> NAME_TO;
	static {
		final Map<String, PlayItemType> m = new HashMap<String, PlayItemType>(values().length);
		for (final PlayItemType t : values()) {
			m.put(t.name().toUpperCase(Locale.ENGLISH), t);
		}
		NAME_TO = Collections.unmodifiableMap(m);
	}

	public static PlayItemType parse(final String name) {
		if (name == null) return null;
		return NAME_TO.get(name.toUpperCase(Locale.ENGLISH));
	}

}
