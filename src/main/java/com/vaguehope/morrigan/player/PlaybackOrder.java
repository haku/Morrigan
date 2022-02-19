package com.vaguehope.morrigan.player;

public enum PlaybackOrder {

	SEQUENTIAL() {
		@Override
		public String toString () {
			return "sequential";
		}
	},

	RANDOM() {
		@Override
		public String toString () {
			return "random";
		}
	},

	BYSTARTCOUNT() {
		@Override
		public String toString () {
			return "by start-count";
		}
	},

	BYLASTPLAYED() {
		@Override
		public String toString () {
			return "by last-played";
		}
	},

	FOLLOWTAGS() {
		@Override
		public String toString () {
			return "follow tags";
		}
	},

	MANUAL() {
		@Override
		public String toString () {
			return "manual";
		}
	},
	;

	public static String joinLabels (final String sep) {
		final PlaybackOrder[] a = values();
		final StringBuilder b = new StringBuilder(a[0].toString());
		for (int i = 1; i < a.length; i++) {
			b.append(sep).append(a[i].toString());
		}
		return b.toString();
	}

	public static PlaybackOrder forceParsePlaybackOrder (final String s) {
		final String arg = s.toLowerCase();
		for (final PlaybackOrder o : values()) {
			if (o.toString().toLowerCase().contains(arg)) {
				return o;
			}
		}
		return null;
	}

	public static PlaybackOrder parsePlaybackOrderByName (final String s) {
		for (final PlaybackOrder o : values()) {
			if (s.equals(o.name())) return o;
		}
		throw new IllegalArgumentException("Unknown order mode name: " + s);
	}

}