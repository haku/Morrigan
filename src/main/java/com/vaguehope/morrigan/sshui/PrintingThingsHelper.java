package com.vaguehope.morrigan.sshui;

import java.text.DateFormat;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.PlayItem;
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

		if (p instanceof LocalPlayer && ((LocalPlayer) p).isProxy()) msg.append(" @ ").append(p.getName());

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
		final PlayItem currentItem = p.getCurrentItem();
		return currentItem != null && currentItem.hasTrack() ? currentItem.getTrack().getTitle() : "";
	}

	public static String listTitleAndOrder(final Player p) {
		final PlayItem currentItem = p.getCurrentItem();
		if (currentItem != null && currentItem.hasList()) {
			return String.format("%s %s", currentItem.getList().getListName(), p.getPlaybackOrder());
		}
		return String.valueOf(p.getPlaybackOrder());
	}

	public static String summariseItemWithPlayCounts (final IMediaTrackList<?> list, final IMediaTrack item, final DateFormat dateFormat) throws MorriganException {
		if (item.getStartCount() > 0 || item.getEndCount() > 0) {
			return String.format("%s/%s %s %s",
					item.getStartCount(), item.getEndCount(),
					item.getDateLastPlayed() == null ? "" : dateFormat.format(item.getDateLastPlayed()),
					PrintingThingsHelper.join(list.getTags(item), ", ", t -> t.getTag()));
		}
		return PrintingThingsHelper.join(list.getTags(item), ", ", t -> t.getTag());
	}

	public static String summariseItemTags (final IMediaTrackList<?> list, final IMediaTrack item) throws MorriganException {
		return PrintingThingsHelper.join(list.getTags(item), ", ", t -> t.getTag());
	}

	public static String summariseTags (final Player player) {
		final PlayItem playItem = player.getCurrentItem();
		if (playItem != null && playItem.hasTrack()) {
			final IMediaTrackList<? extends IMediaTrack> list = player.getCurrentList();
			if (list != null) {
				try {
					final List<MediaTag> tags = list.getTags(playItem.getTrack()); // TODO cache this?
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

	public static String dbSummary (final IMixedMediaDb db) {
		final StringBuilder msg = new StringBuilder();
		msg.append(db.getCount());
		msg.append(" items totaling ");
		final DurationData d = db.getTotalDuration();
		if (!d.isComplete()) {
			msg.append("more than ");
		}
		msg.append(TimeHelper.formatTimeSeconds(d.getDuration()));
		msg.append(".");
		return msg.toString();
	}

	public static String sortSummary (final IMixedMediaDb db) {
		final IDbColumn col = db.getSort();
		return String.format("%s %s.",
				col != null ? col.getHumanName() : "(unknown)",
				db.getSortDirection());
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
