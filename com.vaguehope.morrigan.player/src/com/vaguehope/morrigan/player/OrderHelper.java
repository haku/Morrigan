package com.vaguehope.morrigan.player;

import java.util.Date;
import java.util.List;
import java.util.Random;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;

/**
 * TODO move this to internal package. TODO make one big enum based on an
 * interface?
 */
public final class OrderHelper {

	private OrderHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	// TODO move to own class next to IPlayerAbstact that uses it.
	public static enum PlaybackOrder {

		SEQUENTIAL(0) {
			@Override
			public String toString () {
				return "sequential";
			}
		},

		RANDOM(1) {
			@Override
			public String toString () {
				return "random";
			}
		},

		BYSTARTCOUNT(2) {
			@Override
			public String toString () {
				return "by start-count";
			}
		},

		BYLASTPLAYED(3) {
			@Override
			public String toString () {
				return "by last-played";
			}
		},

		MANUAL(4) {
			@Override
			public String toString () {
				return "manual";
			}
		}
		;

		private int n;

		private PlaybackOrder (final int n) {
			this.n = n;
		}

		public int getN () {
			return this.n;
		}

		public static String joinLabels (final String sep) {
			final PlaybackOrder[] a = values();
			final StringBuilder b = new StringBuilder(a[0].toString());
			for (int i = 1; i < a.length; i++) {
				b.append(sep).append(a[i].toString());
			}
			return b.toString();
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static PlaybackOrder parsePlaybackOrder (final String s) {
		for (final PlaybackOrder o : PlaybackOrder.values()) {
			if (s.equals(o.toString())) return o;
		}
		throw new IllegalArgumentException("Unknown order mode toString: " + s);
	}

	public static PlaybackOrder forceParsePlaybackOrder (final String s) {
		final String arg = s.toLowerCase();
		for (final PlaybackOrder o : PlaybackOrder.values()) {
			if (o.toString().toLowerCase().contains(arg)) {
				return o;
			}
		}
		return null;
	}

	public static PlaybackOrder parsePlaybackOrderByName (final String s) {
		for (final PlaybackOrder o : PlaybackOrder.values()) {
			if (s.equals(o.name())) return o;
		}
		throw new IllegalArgumentException("Unknown order mode name: " + s);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static IMediaTrack getNextTrack (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack track, final PlaybackOrder mode) {
		if (list.getCount() <= 0) return null;

		switch (mode) {
			case SEQUENTIAL:
				return getNextTrackSequencial(list, track);

			case RANDOM:
				return getNextTrackRandom(list, track);

			case BYSTARTCOUNT:
				return getNextTrackByStartCount(list, track);

			case BYLASTPLAYED:
				return getNextTrackByLastPlayedDate(list, track);

			case MANUAL:
				return null;

			default:
				throw new IllegalArgumentException();

		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static IMediaTrack getNextTrackSequencial (final IMediaTrackList<? extends IMediaTrack> list, final IMediaItem track) {
		IMediaTrack ret = null;
		final List<? extends IMediaTrack> mediaTracks = list.getMediaItems();

		int i;
		if (track != null && mediaTracks.contains(track)) {
			i = mediaTracks.indexOf(track) + 1;

		}
		else {
			// With no other info, might as well start at the beginning.
			i = 0;
		}

		int tracksTried = 0;
		while (true) {
			if (i >= mediaTracks.size()) i = 0;

			if (validChoice(mediaTracks.get(i))) {
				break;
			}

			tracksTried++;
			if (tracksTried >= list.getCount()) {
				i = -1;
				break;
			}

			i++;
		}

		if (i > 0) {
			ret = mediaTracks.get(i);
		}

		return ret;
	}

	private static IMediaTrack getNextTrackRandom (final IMediaTrackList<? extends IMediaTrack> list, final IMediaItem current) {
		final Random generator = new Random();
		final List<? extends IMediaTrack> mediaTracks = list.getMediaItems();

		int n = 0;
		for (final IMediaTrack i : mediaTracks) {
			if (validChoice(i, current)) {
				n++;
			}
		}
		if (n == 0) return null;

		long x = Math.round(generator.nextDouble() * n);
		for (final IMediaTrack i : mediaTracks) {
			if (validChoice(i, current)) {
				x--;
				if (x <= 0) {
					return i;
				}
			}
		}

		throw new RuntimeException("Failed to find next track.  This should not happen.");
	}

	private static IMediaTrack getNextTrackByStartCount (final IMediaTrackList<? extends IMediaTrack> list, final IMediaItem current) {
		IMediaTrack ret = null;
		final List<? extends IMediaTrack> tracks = list.getMediaItems();

		// Find highest play count.
		long maxPlayCount = -1;
		for (final IMediaTrack i : tracks) {
			if (i.getStartCount() > maxPlayCount && validChoice(i, current)) {
				maxPlayCount = i.getStartCount();
			}
		}
		if (maxPlayCount < 0) { // No playable items.
			return null;
		}
		maxPlayCount = maxPlayCount + 1;

		// Find sum of all selection indicies.
		long selIndexSum = 0;
		for (final IMediaTrack i : tracks) {
			if (validChoice(i, current)) {
				selIndexSum = selIndexSum + (maxPlayCount - i.getStartCount());
			}
		}

		// Generate target selection index.
		final Random generator = new Random();
		long targetIndex = Math.round(generator.nextDouble() * selIndexSum);

		// Find the target item.
		for (final IMediaTrack i : tracks) {
			if (validChoice(i, current)) {
				targetIndex = targetIndex - (maxPlayCount - i.getStartCount());
				if (targetIndex <= 0) {
					ret = i;
					break;
				}
			}
		}

		if (ret == null || !ret.isPlayable()) {
			throw new RuntimeException("Failed to correctly find next track.  This should not happen.");
		}

		return ret;
	}

	private static IMediaTrack getNextTrackByLastPlayedDate (final IMediaTrackList<? extends IMediaTrack> list, final IMediaTrack current) {
		IMediaTrack ret = null;
		final List<? extends IMediaTrack> tracks = list.getMediaItems();
		final Date now = new Date();

		// Find oldest date.
		Date maxAge = new Date();
		int n = 0;
		for (final IMediaTrack i : tracks) {
			if (validChoice(i, current)) {
				if (i.getDateLastPlayed() != null && i.getDateLastPlayed().before(maxAge)) {
					maxAge = i.getDateLastPlayed();
				}
				n++;
			}
		}
		if (n == 0) { // No playable items.
			return null;
		}
		final long maxAgeDays = dateDiffDays(maxAge, now);

		// Build sum of all selection-indicies in units of days.
		long sumAgeDays = 0;
		for (final IMediaTrack i : tracks) {
			if (validChoice(i, current)) {
				if (i.getDateLastPlayed() != null) {
					sumAgeDays = sumAgeDays + dateDiffDays(i.getDateLastPlayed(), now);
				}
				else {
					sumAgeDays = sumAgeDays + maxAgeDays;
				}
			}
		}

		// Generate target selection index.
		final Random generator = new Random();
		long targetIndex = Math.round(generator.nextDouble() * sumAgeDays);

		// Find the target item.
		for (final IMediaTrack i : tracks) {
			if (validChoice(i, current)) {
				if (i.getDateLastPlayed() != null) {
					targetIndex = targetIndex - dateDiffDays(i.getDateLastPlayed(), now);
				}
				else {
					targetIndex = targetIndex - maxAgeDays;
				}
				if (targetIndex <= 0) {
					ret = i;
					break;
				}
			}
		}

		if (ret == null || !ret.isPlayable()) {
			throw new RuntimeException("Failed to correctly find next track.  This should not happen.  targetIndex=" + targetIndex);
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static boolean validChoice (final IMediaTrack i) {
		return i.isEnabled() && i.isPlayable() && !i.isMissing();
	}

	private static boolean validChoice (final IMediaTrack i, final IMediaItem current) {
		return i.isEnabled() && i.isPlayable() && !i.isMissing() && i != current;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static long dateDiffDays (final Date olderDate, final Date newerDate) {
		long l = (newerDate.getTime() - olderDate.getTime()) / 86400000;
		if (l < 1) l = 1;
		return l;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
