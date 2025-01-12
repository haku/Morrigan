package com.vaguehope.morrigan.sshui;

import java.text.DateFormat;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.SortColumn;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerQueue;
import com.vaguehope.morrigan.util.TimeHelper;

public final class PrintingThingsHelper {

	private static final Logger LOG = LoggerFactory.getLogger(PrintingThingsHelper.class);

	private PrintingThingsHelper () {
		throw new AssertionError();
	}

	public static String playerStateMsg (final Player p) {
		final StringBuilder msg = new StringBuilder();
		msg.append(playerStateTitle(p));
		final long currentPosition = p.getCurrentPosition();
		if (currentPosition >= 0) {
			final int currentTrackDuration = p.getCurrentTrackDuration();
			msg.append(" ").append(TimeHelper.formatTimeSeconds(currentPosition));
			if (currentTrackDuration > 0) {
				msg.append(" of ").append(TimeHelper.formatTimeSeconds(currentTrackDuration));
			}
		}

		return msg.toString();
	}

	public static String playerStateTitle(final Player p) {
		switch (p.getPlayState()) {
			case PLAYING:
				return "Playing";
			case PAUSED:
				return "Paused";
			case LOADING:
				return "Loading";
			case STOPPED:
				return "Stopped";
			default:
				return "Unknown";
		}
	}

	public static String volumeMsg(final Player p) {
		final Integer vol = p.getVoume();
		final Integer maxVol = p.getVoumeMaxValue();
		if (vol == null) return "";

		String msg = "Vol " + vol;
		if (maxVol == 100) {
			msg += "%";
		}
		else {
			msg += "/" + maxVol;
		}
		msg += ".";
		return msg;
	}

	public static String playingItemTitle (final Player p) {
		final PlayItem item = p.getCurrentItem();
		if (item == null) return "";
		return item.resolveTitle(null);
	}

	public static String listTitleAndOrder(final Player p) {
		final StringBuilder s = new StringBuilder();

		final PlayItem item = p.getCurrentItem();
		if (item != null && item.hasList()) {
			s.append(item.getListTitle());
		}

		if (s.length() > 0) s.append(" ");
		s.append(p.getPlaybackOrder());

		PlaybackOrder override = p.getPlaybackOrderOverride();
		if (override != null) s.append(" (").append(override).append(")");

		return s.toString();
	}

	public static String summariseItemWithPlayCounts (final MediaList list, final MediaItem item, final DateFormat dateFormat) throws MorriganException {
		if (item.getStartCount() > 0 || item.getEndCount() > 0) {
			return String.format("%s/%s %s %s",
					item.getStartCount(), item.getEndCount(),
					item.getDateLastPlayed() == null ? "" : dateFormat.format(item.getDateLastPlayed()),
					PrintingThingsHelper.join(list.getTags(item), ", ", t -> t.getTag()));
		}
		return PrintingThingsHelper.join(list.getTags(item), ", ", t -> t.getTag());
	}

	public static String summariseItemTags (final MediaList list, final MediaItem item) throws MorriganException {
		return PrintingThingsHelper.join(list.getTags(item), ", ", t -> t.getTag());
	}

	public static String summariseTags (final Player player) {
		final PlayItem playItem = player.getCurrentItem();
		if (playItem != null && playItem.isReady() && playItem.hasListAndItem()) {
			final MediaList list = playItem.getList();
			if (list != null) {
				try {
					final List<MediaTag> tags = list.getTags(playItem.getItem()); // TODO cache this?
					return join(tags, ", ", t -> t.getTag());
				}
				catch (final MorriganException e) {
					LOG.warn("Failed to read tags: " + playItem, e);
					return "(tags unavailable)";
				}
			}
		}
		return "";
	}

	public static String queueSummary (final PlayerQueue queue) {
		final int size = queue.size();
		if (size == 0) return "Queue is empty.";
		final DurationData d = queue.getQueueTotalDuration();
		return String.format("Queue: %s items totaling %s%s.",
				size,
				d.isComplete() ? "" : "more than ",
				TimeHelper.formatTimeSeconds(d.getDuration()));
	}

	public static String dbSummary (final MediaDb db) {
		final StringBuilder msg = new StringBuilder();
		msg.append(db.size());
		msg.append(" items totaling ");
		final DurationData d = db.getTotalDuration();
		if (!d.isComplete()) {
			msg.append("more than ");
		}
		msg.append(TimeHelper.formatTimeSeconds(d.getDuration()));
		msg.append(".");
		return msg.toString();
	}

	public static String sortSummary (final MediaList list) {
		final SortColumn col = list.getSortColumn();
		return String.format("%s %s.",
				col != null ? col.getUiName() : "(unknown)",
				list.getSortDirection());
	}

	public static String scrollSummary (final int count, final int pageSize, final int scrollTop) {
		if (scrollTop == 0) {
			if (count < pageSize) return "All";
			return "Top";
		}
		if (scrollTop >= count - pageSize) return "Bot";
		return String.format("%1$2s%%", (int) (((scrollTop + (pageSize / 2)) / (double) count) * 100));
	}

	public static <T> String join (final Collection<T> arr, final String sep, Function<T, String> toStr) {
		final StringBuilder s = new StringBuilder();
		for (final T obj : arr) {
			if (s.length() > 0) s.append(sep);
			s.append(toStr.apply(obj));
		}
		return s.toString();
	}

}
