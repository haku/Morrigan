package com.vaguehope.morrigan.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.SortColumn;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.LimitedRecentSet;
import com.vaguehope.morrigan.util.MnLogger;

public class OrderResolver {

	private static final int FOLLOWTAGS_MAX_RESULTS_PER_TAG_SEARCH = 200;
	private static final long FOLLOWTAGS_MIN_TIME_SINCE_LAST_PLAYED_MILLIS = TimeUnit.DAYS.toMillis(1);
	private static final int FOLLOWTAGS_MAX_TAG_HISTORY = 3;
	private static final MnLogger LOG = MnLogger.make(OrderResolver.class);

	private final LimitedRecentSet<String> recentlyFollowedTags = new LimitedRecentSet<>(FOLLOWTAGS_MAX_TAG_HISTORY);

	public MediaItem getNextTrack (final MediaList list, final MediaItem track, final PlaybackOrder mode) {
		if (list == null || list.size() <= 0) return null;

		switch (mode) {
			case SEQUENTIAL:
				return getNextTrackSequencial(list, track);

			case RANDOM:
				return getNextTrackRandom(list, track);

			case BYSTARTCOUNT:
				return getNextTrackByStartCount(list, track, 1.0d);

			case BYSTARTCOUNT_EXP:
				return getNextTrackByStartCount(list, track, 1.3d);

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

	private static MediaItem getNextTrackSequencial (final MediaList list, final MediaItem track) {
		MediaItem ret = null;
		final List<MediaItem> mediaTracks = list.getMediaItems();

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
			if (tracksTried >= list.size()) {
				i = -1;
				break;
			}

			i++;
		}

		if (i >= 0) {
			ret = mediaTracks.get(i);
		}

		return ret;
	}

	private static MediaItem getNextTrackRandom (final MediaList list, final MediaItem current) {
		final Random generator = new Random();
		final List<MediaItem> mediaTracks = list.getMediaItems();

		int n = 0;
		for (final MediaItem i : mediaTracks) {
			if (validChoice(i, current)) {
				n++;
			}
		}
		if (n == 0) return null;

		long x = Math.round(generator.nextDouble() * n);
		for (final MediaItem i : mediaTracks) {
			if (validChoice(i, current)) {
				x--;
				if (x <= 0) {
					return i;
				}
			}
		}

		throw new RuntimeException("Failed to find next track.  This should not happen.");
	}

	private static MediaItem getNextTrackByStartCount (final MediaList list, final MediaItem current, double weightPower) {
		MediaItem ret = null;
		final List<MediaItem> tracks = list.getMediaItems();

		// Find highest play count.
		long maxPlayCount = -1;
		for (final MediaItem i : tracks) {
			if (i.getStartCount() > maxPlayCount && validChoice(i, current)) {
				maxPlayCount = i.getStartCount();
			}
		}
		if (maxPlayCount < 0) { // No playable items.
			return null;
		}
		maxPlayCount = maxPlayCount + 1;

		// Find sum of all selection indices.
		long selIndexSum = 0;
		for (final MediaItem i : tracks) {
			if (validChoice(i, current)) {
				selIndexSum += trackCountWeight(i.getStartCount(), maxPlayCount, weightPower);
			}
		}

		// Generate target selection index.
		final Random generator = new Random();
		long targetIndex = Math.round(generator.nextDouble() * selIndexSum);

		// Find the target item.
		for (final MediaItem i : tracks) {
			if (validChoice(i, current)) {
				targetIndex = targetIndex - trackCountWeight(i.getStartCount(), maxPlayCount, weightPower);
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

	private static long trackCountWeight(long count, long maxCount, double power) {
		return (long) Math.pow(maxCount - count, power);
	}

	private static MediaItem getNextTrackByLastPlayedDate (final MediaList list, final MediaItem current) {
		return getNextTrackByLastPlayedDate(list.getMediaItems(), current);
	}

	private static MediaItem getNextTrackByLastPlayedDate (final Collection<MediaItem> tracks, final MediaItem current) {
		final Date now = new Date();

		// Find oldest date.
		Date maxAge = new Date();
		int n = 0;
		for (final MediaItem i : tracks) {
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
		for (final MediaItem i : tracks) {
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
		MediaItem ret = null;
		for (final MediaItem i : tracks) {
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

	private MediaItem getNextTrackFollowTags (final MediaList list, final MediaItem current) {
		try {
			return getNextTrackFollowTagsOrThrow(list, current);
		}
		catch (final MorriganException e) {
			throw new IllegalStateException(e);
		}
	}

	private MediaItem getNextTrackFollowTagsOrThrow (final MediaList list, final MediaItem current) throws MorriganException, DbException {
		// Can not follow tags if no current item.
		if (current == null) return getNextTrackByLastPlayedDate(list, current);

		if (!(list instanceof MediaDb)) throw new IllegalArgumentException("Only DB lists supported.");
		final MediaDb db = (MediaDb) list;

		final Random rnd = new Random();

		final List<String> highPri = new ArrayList<>();
		final List<String> lowPri = new ArrayList<>();

		final List<String> currentItemsTags = manualTagsAsStrings(current.getTags());

		synchronized (this.recentlyFollowedTags) {
			for (String lastTag : this.recentlyFollowedTags) {
				if (currentItemsTags.contains(lastTag)) {
					if (this.recentlyFollowedTags.frequency(lastTag) < 10 + rnd.nextInt(10)) {
						if (rnd.nextBoolean()) {
							highPri.add(lastTag);
						}
						else {
							lowPri.add(lastTag);
						}
					}
					currentItemsTags.remove(lastTag);
				}
			}
		}

		Collections.shuffle(currentItemsTags);

		final List<String> tagsToSearchForInOrder = new ArrayList<>();
		tagsToSearchForInOrder.addAll(highPri);
		tagsToSearchForInOrder.addAll(currentItemsTags);
		tagsToSearchForInOrder.addAll(lowPri);

		for (final String tag : tagsToSearchForInOrder) {
			final List<MediaItem> itemsWithTag = db.search(
					MediaType.TRACK,
					String.format("t=\"%s\"", tag),
					FOLLOWTAGS_MAX_RESULTS_PER_TAG_SEARCH,
					new SortColumn[] { SortColumn.DATE_LAST_PLAYED },
					new SortDirection[] { SortDirection.ASC },
					false);

			for (final Iterator<MediaItem> ittr = itemsWithTag.iterator(); ittr.hasNext();) {
				final Date d = ittr.next().getDateLastPlayed();
				if (d == null) continue;
				if (System.currentTimeMillis() - d.getTime() < FOLLOWTAGS_MIN_TIME_SINCE_LAST_PLAYED_MILLIS) ittr.remove();
			}

			if (itemsWithTag.size() > 0) {
				// byLastPlayedDate() handles not selecting current again.
				final MediaItem item = getNextTrackByLastPlayedDate(itemsWithTag, current);
				if (item != null) {
					LOG.i("{} => {}", tag, item.getTitle());
					this.recentlyFollowedTags.push(tag);
					return item;
				}
			}
		}

		LOG.i("Jump.");

		// Fall back if no tags to follow.
		return getNextTrackByLastPlayedDate(list, current);
	}

	private static List<String> manualTagsAsStrings (final List<MediaTag> tags) {
		final List<String> ret = new ArrayList<>(tags.size());
		for (final MediaTag tag : tags) {
			if (tag.getType() == MediaTagType.MANUAL) ret.add(tag.getTag());
		}
		return ret;
	}

	private static boolean validChoice (final MediaItem i) {
		return i.isEnabled() && i.isPlayable() && !i.isMissing();
	}

	private static boolean validChoice (final MediaItem i, final MediaItem current) {
		return i.isEnabled() && i.isPlayable() && !i.isMissing() && !i.equals(current);
	}

	private static long dateDiffDays (final Date olderDate, final Date newerDate) {
		long l = (newerDate.getTime() - olderDate.getTime()) / 86400000;
		if (l < 1) l = 1;
		return l;
	}

}
