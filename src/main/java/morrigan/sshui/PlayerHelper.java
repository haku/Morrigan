package morrigan.sshui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.ActionListDialog;

import morrigan.model.media.MediaItem;
import morrigan.model.media.MediaList;
import morrigan.player.PlayItem;
import morrigan.player.Player;
import morrigan.player.PlayerQueue;
import morrigan.player.PlayerSorter;

public final class PlayerHelper {

	private PlayerHelper () {
		throw new AssertionError();
	}

	public static void shuffleAndEnqueue (final MediaList db, final List<MediaItem> tracks, final Player player) {
		final List<MediaItem> shuffeledList = new ArrayList<>(tracks);
		Collections.shuffle(shuffeledList);
		enqueueAll(db, shuffeledList, player);
	}

	public static void playAll (final MediaList db, final List<MediaItem> tracks, final Player player) {
		final List<PlayItem> items = new ArrayList<>();
		for (final MediaItem track : tracks) {
			items.add(PlayItem.makeReady(db, track));
		}
		player.getQueue().addToQueue(items);
		player.getQueue().moveInQueueEnd(items, false);
		player.nextTrack();
	}

	public static void enqueueAll (final MediaList db, final List<MediaItem> tracks, final Player player) {
		final PlayerQueue queue = player.getQueue();
		for (final MediaItem track : tracks) {
			queue.addToQueue(PlayItem.makeReady(db, track));
		}
	}

	public static Player askWhichPlayer (final WindowBasedTextGUI gui, final String title, final Player defaultPlayer, final Collection<Player> players) {
		if (defaultPlayer != null) return defaultPlayer;
		if (players == null || players.size() < 1) {
			return null;
		}
		else if (players.size() == 1) {
			return players.iterator().next();
		}

		final List<Player> sorted = new ArrayList<>(players);
		sorted.sort(PlayerSorter.STATE);

		final AtomicReference<Player> ret = new AtomicReference<>();
		final List<Runnable> actions = new ArrayList<>();
		for (final Player player : sorted) {
			actions.add(new Runnable() {
				@Override
				public String toString () {
					return player.getName() + "  (" + PrintingThingsHelper.playerStateTitle(player) + ")";
				}

				@Override
				public void run () {
					ret.set(player);
				}
			});
		}
		ActionListDialog.showDialog(gui, title, "Select player",
				actions.toArray(new Runnable[actions.size()]));
		return ret.get();
	}

}
