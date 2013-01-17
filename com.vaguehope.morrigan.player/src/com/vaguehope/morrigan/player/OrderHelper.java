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
	// TODO add "STOP" entry to not auto-advance.
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

		private PlaybackOrder (int n) {
			this.n = n;
		}

		public int getN () {
			return this.n;
		}

		public static String joinLabels (String sep) {
			PlaybackOrder[] a = values();
			StringBuilder b = new StringBuilder(a[0].toString());
			for (int i = 1; i < a.length; i++) {
				b.append(sep).append(a[i].toString());
			}
			return b.toString();
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static PlaybackOrder parsePlaybackOrder (String s) {
		for (PlaybackOrder o : PlaybackOrder.values()) {
			if (s.equals(o.toString())) return o;
		}
		throw new IllegalArgumentException("Unknown order mode toString: " + s);
	}

	public static PlaybackOrder forceParsePlaybackOrder (String s) {
		String arg = s.toLowerCase();
		for (PlaybackOrder o : PlaybackOrder.values()) {
			if (o.toString().toLowerCase().contains(arg)) {
				return o;
			}
		}
		return null;
	}

	public static PlaybackOrder parsePlaybackOrderByName (String s) {
		for (PlaybackOrder o : PlaybackOrder.values()) {
			if (s.equals(o.name())) return o;
		}
		throw new IllegalArgumentException("Unknown order mode name: " + s);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	// TODO take into account disabled and missing tracks.
	public static IMediaTrack getNextTrack (IMediaTrackList<? extends IMediaTrack> list, IMediaTrack track, PlaybackOrder mode) {
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

	private static IMediaTrack getNextTrackSequencial (IMediaTrackList<? extends IMediaTrack> list, IMediaItem track) {
		IMediaTrack ret = null;
		List<? extends IMediaTrack> mediaTracks = list.getMediaItems();

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

	private static IMediaTrack getNextTrackRandom (IMediaTrackList<? extends IMediaTrack> list, IMediaItem current) {
		Random generator = new Random();
		List<? extends IMediaTrack> mediaTracks = list.getMediaItems();

		int n = 0;
		for (IMediaTrack i : mediaTracks) {
			if (validChoice(i, current)) {
				n++;
			}
		}
		if (n == 0) return null;

		long x = Math.round(generator.nextDouble() * n);
		for (IMediaTrack i : mediaTracks) {
			if (validChoice(i, current)) {
				x--;
				if (x <= 0) {
					return i;
				}
			}
		}

		throw new RuntimeException("Failed to find next track.  This should not happen.");
	}

	private static IMediaTrack getNextTrackByStartCount (IMediaTrackList<? extends IMediaTrack> list, IMediaItem current) {
		IMediaTrack ret = null;
		List<? extends IMediaTrack> tracks = list.getMediaItems();

		// Find highest play count.
		long maxPlayCount = -1;
		for (IMediaTrack i : tracks) {
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
		for (IMediaTrack i : tracks) {
			if (validChoice(i, current)) {
				selIndexSum = selIndexSum + (maxPlayCount - i.getStartCount());
			}
		}

		// Generate target selection index.
		Random generator = new Random();
		long targetIndex = Math.round(generator.nextDouble() * selIndexSum);

		// Find the target item.
		for (IMediaTrack i : tracks) {
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

	private static IMediaTrack getNextTrackByLastPlayedDate (IMediaTrackList<? extends IMediaTrack> list, IMediaTrack current) {
		IMediaTrack ret = null;
		List<? extends IMediaTrack> tracks = list.getMediaItems();
		Date now = new Date();

		// Find oldest date.
		Date maxAge = new Date();
		int n = 0;
		for (IMediaTrack i : tracks) {
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
		long maxAgeDays = dateDiffDays(maxAge, now);

		// Build sum of all selection-indicies in units of days.
		long sumAgeDays = 0;
		for (IMediaTrack i : tracks) {
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
		Random generator = new Random();
		long targetIndex = Math.round(generator.nextDouble() * sumAgeDays);

		// Find the target item.
		for (IMediaTrack i : tracks) {
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

	private static boolean validChoice (IMediaTrack i) {
		return i.isEnabled() && i.isPlayable() && !i.isMissing();
	}

	private static boolean validChoice (IMediaTrack i, IMediaItem current) {
		return i.isEnabled() && i.isPlayable() && !i.isMissing() && i != current;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static long dateDiffDays (Date olderDate, Date newerDate) {
		long l = (newerDate.getTime() - olderDate.getTime()) / 86400000;
		if (l < 1) l = 1;
		return l;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
