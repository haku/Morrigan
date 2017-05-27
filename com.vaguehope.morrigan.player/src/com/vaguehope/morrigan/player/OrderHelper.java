package com.vaguehope.morrigan.player;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMixedMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.util.MnLogger;
import com.vaguehope.sqlitewrapper.DbException;

public final class OrderHelper {

	private static final int FOLLOWTAGS_MAX_RESULTS_PER_TAG_SEARCH = 200;
	private static final long FOLLOWTAGS_MIN_TIME_SINCE_LAST_PLAYED_MILLIS = TimeUnit.DAYS.toMillis(1);
	private static final MnLogger LOG = MnLogger.make(OrderHelper.class);

	private OrderHelper () {}

	public static IMediaTrack getNextTrack (final IMediaItemList<? extends IMediaTrack> list, final IMediaTrack track, final PlaybackOrder mode) {
		if (list == null || list.getCount() <= 0) return null;

		switch (mode) {
			case SEQUENTIAL:
				return getNextTrackSequencial(list, track);

			case RANDOM:
				return getNextTrackRandom(list, track);

			case BYSTARTCOUNT:
				return getNextTrackByStartCount(list, track);

			case BYLASTPLAYED:
				return getNextTrackByLastPlayedDate(list, track);

			case FOLLOWTAGS:
				return getNextTrackFollowTags(list, track);

			case MANUAL:
				return null;

			default:
				throw new IllegalArgumentException();

		}
	}

	private static IMediaTrack getNextTrackSequencial (final IMediaItemList<? extends IMediaTrack> list, final IMediaItem track) {
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

	private static IMediaTrack getNextTrackRandom (final IMediaItemList<? extends IMediaTrack> list, final IMediaItem current) {
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

	private static IMediaTrack getNextTrackByStartCount (final IMediaItemList<? extends IMediaTrack> list, final IMediaItem current) {
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

	private static IMediaTrack getNextTrackByLastPlayedDate (final IMediaItemList<? extends IMediaTrack> list, final IMediaTrack current) {
		return getNextTrackByLastPlayedDate(list.getMediaItems(), current);
	}

	private static IMediaTrack getNextTrackByLastPlayedDate (final Collection<? extends IMediaTrack> tracks, final IMediaTrack current) {
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
		IMediaTrack ret = null;
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

	private static IMediaTrack getNextTrackFollowTags (final IMediaItemList<? extends IMediaTrack> list, final IMediaTrack current) {
		try {
			return getNextTrackFollowTagsOrThrow(list, current);
		}
		catch (final MorriganException e) {
			throw new IllegalStateException(e);
		}
		catch (final DbException e) {
			throw new IllegalStateException(e);
		}
	}

	private static IMediaTrack getNextTrackFollowTagsOrThrow (final IMediaItemList<? extends IMediaTrack> list, final IMediaTrack current) throws MorriganException, DbException {
		// Can not follow tags if no current item.
		if (current == null) return getNextTrackByLastPlayedDate(list, current);

		if (!(list instanceof IMixedMediaDb)) throw new IllegalArgumentException("Only DB lists supported.");
		final IMixedMediaDb db = (IMixedMediaDb) list;

		final List<MediaTag> currentTags = db.getTags(current);
		// TODO make this part of getTags()?
		for (final Iterator<MediaTag> ittr = currentTags.iterator(); ittr.hasNext();) {
			if (ittr.next().getType() != MediaTagType.MANUAL) ittr.remove();
		}

		// TODO make this try and use the same tag as list time if possible?
		// Perhaps be sticky to one tag until the choices don't look so good,
		// then getNextTrackByLastPlayedDate() across all the candidates?
		Collections.shuffle(currentTags);

		for (final MediaTag tag : currentTags) {
			final List<? extends IMediaTrack> itemsWithTag = db.simpleSearchMedia(
					MediaType.TRACK,
					String.format("t=\"%s\"", tag.getTag()),
					FOLLOWTAGS_MAX_RESULTS_PER_TAG_SEARCH,
					new IDbColumn[] {
					IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_DLASTPLAY,
					},
					new SortDirection[] { SortDirection.ASC },
					false);

			for (final Iterator<? extends IMediaTrack> ittr = itemsWithTag.iterator(); ittr.hasNext();) {
				final Date d = ittr.next().getDateLastPlayed();
				if (d == null) continue;
				if (System.currentTimeMillis() - d.getTime() < FOLLOWTAGS_MIN_TIME_SINCE_LAST_PLAYED_MILLIS) ittr.remove();
			}

			if (itemsWithTag.size() > 0) {
				// byLastPlayedDate() handles not selecting current again.
				final IMediaTrack item = getNextTrackByLastPlayedDate(itemsWithTag, current);
				if (item != null) {
					LOG.i("{0} => {1}", tag.getTag(), item.getTitle());
					return item;
				}
			}
		}

		// Fall back if no tags to follow.
		return getNextTrackByLastPlayedDate(list, current);
	}

	private static boolean validChoice (final IMediaTrack i) {
		return i.isEnabled() && i.isPlayable() && !i.isMissing();
	}

	private static boolean validChoice (final IMediaTrack i, final IMediaItem current) {
		return i.isEnabled() && i.isPlayable() && !i.isMissing() && i != current;
	}

	private static long dateDiffDays (final Date olderDate, final Date newerDate) {
		long l = (newerDate.getTime() - olderDate.getTime()) / 86400000;
		if (l < 1) l = 1;
		return l;
	}

}
