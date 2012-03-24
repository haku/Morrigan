package com.vaguehope.morrigan.osgiconsole;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.impl.MediaFactoryImpl;
import com.vaguehope.morrigan.player.IPlayerLocal;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.server.AsyncActions;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDb;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbHelper;
import com.vaguehope.morrigan.tasks.AsyncProgressRegister;
import com.vaguehope.morrigan.util.ErrorHelper;
import com.vaguehope.morrigan.util.TimeHelper;

public class MorriganCommandProvider implements CommandProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getHelp() {
		return "---Morrigan---\n" +
				"\tmn [media|m]\n" +
				"\tmn [media|m] [create|c] <dbname>\n" +
				"\tmn [media|m] [create|c] [remote|r] <dbname> <pass>\n" +
				"\tmn [media|m] [add|a] <dir> <q1>\n" +
				"\tmn [media|m] [update|u] <q1>\n" +
				"\tmn [media|m] [sync|s] <remote q1> <local q1>\n" +
				"\tmn [media|m] <q1> [<q2>]\n" +
				"\tmn [players|player|p]\n" +
				"\tmn [player|p] 0 [play|queue] [<q1> [<q2>]]\n" +
				"\tmn [player|p] 0 [queue|q] clear\n" +
				"\tmn [player|p] 0 [pause|stop|next]\n" +
				"\tmn [player|p] 0 [order|o] [<order>]\n" +
				"\tmn play [<q1> [<q2>]]\n" +
				"\tmn [queue|q] [<q1> [<q2>]|clear]\n" +
				"\tmn [pause|stop|s|next|n]\n" +
				"\tmn st\n" +
				"\tNOTE 1: <q1> = list, <q2> = item in <q1>.\n" +
				"\tNOTE 2: Only omit player ID when there is only one player.\n";
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void _mn (CommandInterpreter ci) {
		List<String> args = new LinkedList<String>();
		String arg = null;
		while ((arg = ci.nextArgument()) != null) {
			args.add(arg);
		}
		if (args.size() < 1) {
			ci.println("No method specified.");
			return;
		}

		String cmd = args.remove(0);
		if (cmd.equals("st")) {
			doStat(ci);
		}
		else if (cmd.equals("m") || cmd.equals("media")) {
			doMedia(ci, args);
		}
		else if (cmd.equals("p") || cmd.equals("players") || cmd.equals("player")) {
			doPlayers(ci, args);
		}
		else if (cmd.equals("play")) {
			doPlay(ci, args);
		}
		else if (cmd.equals("q") || cmd.equals("queue") || cmd.equals("enqueue")) {
			doQueue(ci, args);
		}
		else if (cmd.equals("pause")) {
			doPause(ci);
		}
		else if (cmd.equals("s") || cmd.equals("stop")) {
			doStop(ci);
		}
		else if (cmd.equals("n") || cmd.equals("next")) {
			doNext(ci);
		}
		else if (cmd.equals("h") || cmd.equals("help")) {
			ci.print(this.getHelp()); // already ends with new line.
		}
		else {
			ci.println("Unknown command '"+cmd+"'.");
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	static private void doStat (CommandInterpreter ci) {
		ci.print(AsyncProgressRegister.reportSummary()); // Has own trailing new line.
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	static private void doMedia (CommandInterpreter ci, List<String> args) {
		if (args.size() < 1) {
			doMediaList(ci);
			return;
		}

		String cmd = args.remove(0);
		if (cmd.equals("list")) {
			doMediaList(ci);
		}
		else if (cmd.equals("u") || cmd.equals("update")) {
			doMediaScan(ci, args);
		}
		else if (cmd.equals("c") || cmd.equals("create")) {
			doMediaCreate(ci, args);
		}
		else if (cmd.equals("a") || cmd.equals("add")) {
			doMediaAdd(ci, args);
		}
		else if (cmd.equals("s") || cmd.equals("sync")) {
			doMediaSync(ci, args);
		}
		else {
			String q1 = cmd;
			String q2 = args.size() >= 1 ? args.get(0) : null;

			List<PlayItem> results = null;
			try {
				results = CliHelper.queryForPlayableItems(q1, q2, 10);
			} catch (MorriganException e) {
				ci.println(ErrorHelper.getCauseTrace(e));
				return;
			}

			if (results == null || results.size() < 1) {
				ci.println("No results for query '"+q1+"' '"+q2+"'.");
			}
			else if (results.size() == 1) {
				IMediaTrackList<? extends IMediaTrack> list = results.get(0).list;
				ci.println("Query match: " + list);
				if (results.get(0).list instanceof ILocalMixedMediaDb) {
					ILocalMixedMediaDb mmdb = (ILocalMixedMediaDb) list;
					if (q2 != null && q2.length() > 0) {
						// run query q2.
						try {
							results = CliHelper.queryForPlayableItems(q1, q2, 10);
						} catch (MorriganException e) {
							ci.println(ErrorHelper.getCauseTrace(e));
						}

						if (results == null || results.size() < 1) {
							ci.println("No results for query '"+q1+"' '"+q2+"'.");
						}
						else {
							ci.println("Results for query:");
							for (PlayItem pi : results) {
								ci.println(" > " + pi.toString());
							}
						}
					}
					else { // Print DB info.
						DurationData d = mmdb.getTotalDuration();
						ci.print(" ");
						ci.print(String.valueOf(mmdb.getCount()));
						ci.print(" items totaling ");
						if (!d.isComplete()) ci.print("more than ");
						ci.print(TimeHelper.formatTimeSeconds(d.getDuration()));
						ci.println(".");

						long queryTime = mmdb.getDurationOfLastRead();
						if (queryTime > 0) {
							ci.print(" Query took ");
							ci.print(TimeHelper.formatTimeMiliseconds(queryTime));
							ci.println(" seconds.");
						}

    					List<String> sources;
    					try {
    						sources = mmdb.getSources();
    					} catch (MorriganException e) {
    						ci.println(ErrorHelper.getCauseTrace(e));
    						return;
    					}
    					for (String s : sources) {
    						ci.println(" src > " + s);
    					}
					}
				}
				else {
					// TODO its no ILocalMixedMediaDb.
					ci.println("TODO query types other than ILocalMixedMediaDb.");
				}
			}
			else {
				ci.println("Results for query:");
				for (PlayItem pi : results) {
					ci.println(" > " + pi.toString());
				}
			}
		}
	}

	static private void doMediaList (CommandInterpreter ci) {
		List<MediaListReference> items = new LinkedList<MediaListReference>();
		items.addAll(MediaFactoryImpl.get().getAllLocalMixedMediaDbs());
		items.addAll(RemoteMixedMediaDbHelper.getAllRemoteMmdb());
		for (MediaListReference i : items) {
			ci.println(i.getType() + " " + i.getTitle());
		}
	}

	static private void doMediaCreate (CommandInterpreter ci, List<String> args) {
		if (args.size() >= 1) {
			if ("remote".equals(args.get(0)) || "r".equals(args.get(0))) {
				if (args.size() >= 3) {
					String url = args.get(1);
					String pass = args.get(2);
					IRemoteMixedMediaDb db;
					try {
						db = RemoteMixedMediaDbHelper.createRemoteMmdb(url, pass);
						ci.println("Created MMDB '"+db.getListName()+"'.");
					}
					catch (MalformedURLException e) {
						ci.println("Maoformed URL: " + url);
					}
					catch (MorriganException e) {
						ci.println(ErrorHelper.getCauseTrace(e));
					}
				}
				else {
					ci.println("You must specify a URL and pass for the new remote DB.");
				}
			}
			else {
				String name = args.get(0);
				try {
					ILocalMixedMediaDb mmdb = MediaFactoryImpl.get().createLocalMixedMediaDb(name);
					ci.println("Created MMDB '"+mmdb.getListName()+"'.");
				}
				catch (MorriganException e) {
					ci.println(ErrorHelper.getCauseTrace(e));
				}
			}
		}
		else {
			ci.println("You must specify 'remote' or a name for the new DB.");
		}
	}

	static private void doMediaAdd (CommandInterpreter ci, List<String> args) {
		if (args.size() < 2) {
			ci.println("Not enough arguments.");
			return;
		}

		String dirArg = args.get(0);
		String q1 = args.get(1);

		File dir = new File(dirArg);
		if (!dir.exists()) {
			ci.println("Directory '"+dir.getAbsolutePath()+"' not found.");
			return;
		}

		List<PlayItem> results = null;
		try {
			results = CliHelper.queryForPlayableItems(q1, null, 2);
		}
		catch (MorriganException e) {
			ci.println(ErrorHelper.getCauseTrace(e));
			return;
		}

		if (results == null || results.size() != 1) {
			ci.println("Query '"+q1+"' did not return only one result.");
		}
		else {
			IMediaTrackList<? extends IMediaTrack> list = results.get(0).list;
			if (list instanceof ILocalMixedMediaDb) {
				ILocalMixedMediaDb mmdb = (ILocalMixedMediaDb) list;
				try {
					mmdb.addSource(dir.getAbsolutePath());
				} catch (MorriganException e) {
					ci.println(ErrorHelper.getCauseTrace(e));
					return;
				}
			}
			else if (list instanceof RemoteMixedMediaDb) {
				ci.println("You can not edit the sources for a remote library.");
			}
			else {
				ci.println("Unable to add to the item type of '"+list.getListName()+"'.");
			}
		}
	}

	static private void doMediaScan (CommandInterpreter ci, List<String> args) {
		if (args.size() < 1) {
			ci.println("No query parameter.");
		}
		else {
			String q1 = args.get(0);
			List<PlayItem> results = null;
			try {
				results = CliHelper.queryForPlayableItems(q1, null, 2);
			}
			catch (MorriganException e) {
				ci.println(ErrorHelper.getCauseTrace(e));
				return;
			}

			if (results == null || results.size() != 1) {
				ci.println("Query '"+q1+"' did not return only one result.");
			}
			else {
				IMediaTrackList<? extends IMediaTrack> list = results.get(0).list;
				if (list instanceof ILocalMixedMediaDb) {
					AsyncActions.scheduleMmdbScan((ILocalMixedMediaDb) list);
					ci.println("Scan scheduled.  Use 'mn st' to track progress.");
				}
				else if (list instanceof RemoteMixedMediaDb) {
					AsyncActions.scheduleRemoteMmdbScan((RemoteMixedMediaDb) list);
					ci.println("Scan scheduled.  Use 'mn st' to track progress.");
				}
				else {
					ci.println("Unable to schedule scan for item '"+list.getListName()+"'.");
				}
			}
		}
	}

	static private void doMediaSync (CommandInterpreter ci, List<String> args) {
		if (args.size() < 2) {
			ci.println("Must specify a remote and a local DB.");
		}
		else {
			String rq1 = args.get(0);
			String lq1 = args.get(1);
			try {
				List<PlayItem> rq1Pi = CliHelper.queryForPlayableItems(rq1, null, 2);
				List<PlayItem> lq1Pi = CliHelper.queryForPlayableItems(lq1, null, 2);
				if (rq1Pi == null || rq1Pi.size() != 1) {
					ci.println("Query '" + rq1 + "' did not return only one result.");
				}
				else if (lq1Pi == null || lq1Pi.size() != 1) {
					ci.println("Query '" + lq1 + "' did not return only one result.");
				}
				else {
					IMediaTrackList<? extends IMediaTrack> rl = rq1Pi.get(0).list;
					IMediaTrackList<? extends IMediaTrack> ll = lq1Pi.get(0).list;
					if (!(rl instanceof IRemoteMixedMediaDb)) {
						ci.println("DB '" + rq1 + "' is not a remote DB.");
					}
					else if (!(ll instanceof ILocalMixedMediaDb)) {
						ci.println("DB '" + rq1 + "' is not a local DB.");
					}
					else {
						IRemoteMixedMediaDb rdb = (IRemoteMixedMediaDb) rl;
						ILocalMixedMediaDb ldb = (ILocalMixedMediaDb) ll;
						AsyncActions.syncMetaData(ldb, rdb);
						ci.println("Synchronisation scheduled.  Use 'mn st' to track progress.");
					}
				}
			}
			catch (MorriganException e) {
				ci.println(ErrorHelper.getCauseTrace(e));
			}
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	static private void doPlayers (CommandInterpreter ci, List<String> args) {
		if (args.size() < 1) {
			doPlayersList(ci);
			return;
		}

		String cmd = args.remove(0);
		try {
			int playerId = Integer.parseInt(cmd);
			IPlayerLocal player = PlayerRegister.getLocalPlayer(playerId);
			doPlayersPlayer(ci, player, args);
		}
		catch (NumberFormatException e) {
			// If we only have one player, assume the next param is a cmd.
			if (PlayerRegister.getLocalPlayers().size() == 1) {
				IPlayerLocal player = PlayerRegister.getLocalPlayer(0);
				args.add(0, cmd);
				doPlayersPlayer(ci, player, args);
			}
			else {
				ci.println("Unknown player ID '"+cmd+"'.");
			}
		}
	}

	static private void doPlayersList(CommandInterpreter ci) {
		Collection<IPlayerLocal> players = PlayerRegister.getLocalPlayers();
		ci.println("id\tplayer");
		for (IPlayerLocal p : players) {
			ci.print(String.valueOf( p.getId() ));
			ci.print("\t");
			ci.print(p.getPlayState());

			PlayItem currentItem = p.getCurrentItem();
			if (currentItem != null && currentItem.item != null) {
				ci.print(": ");
				ci.print(currentItem.item.getTitle());
			}

			ci.println();
		}
	}

	static private void doPlayersPlayer (CommandInterpreter ci, IPlayerLocal player, List<String> args) {
		if (args.size() < 1) {
			doPlayersPlayerInfo(ci, player);
			return;
		}

		String cmd = args.remove(0);
		if (cmd.equals("p") || cmd.equals("play")) {
			doPlayersPlayerPlay(ci, player, args, false);
		}
		else if (cmd.equals("q") || cmd.equals("queue") || cmd.equals("enqueue")) {
			doPlayersPlayerPlay(ci, player, args, true);
		}
		else if (cmd.equals("pause")) {
			doPlayersPlayerPause(ci, player);
		}
		else if (cmd.equals("s") || cmd.equals("stop")) {
			doPlayersPlayerStop(ci, player);
		}
		else if (cmd.equals("n") || cmd.equals("next")) {
			doPlayersPlayerNext(ci, player);
		}
		else if (cmd.equals("o") || cmd.equals("order")) {
			doPlayersPlayerOrder(ci, player, args);
		}
		else {
			ci.println("Unknown command '"+cmd+"'.");
		}
	}

	static private void doPlayersPlayerInfo (CommandInterpreter ci, IPlayerLocal player) {
		ci.print("Player ");
		ci.print(String.valueOf( player.getId() ));
		ci.print(": ");
		ci.print(player.getPlayState().toString());
		ci.print(" (");
		ci.print(player.getPlaybackOrder().toString());
		ci.print(")");
		ci.println();

		PlayItem currentItem = player.getCurrentItem();
		String item = (currentItem != null && currentItem.item != null) ? currentItem.item.getTitle() : "";
		ci.println("\titem=" + item);

		IMediaTrackList<? extends IMediaTrack> currentList = player.getCurrentList();
		String list = currentList != null ? currentList.getListName() : "";
		ci.println("\tlist=" + list);

		ci.println("\tqueue=" + player.getQueueList().size() + " items.");
	}

	static private void doPlayersPlayerPlay (CommandInterpreter ci, IPlayerLocal player, List<String> args, boolean addToQueue) {
		if (args.size() < 1) {
			if (addToQueue) {
				doPlayersPlayerPrintQueue(ci, player);
			}
			else {
				if (player.getPlayState() == PlayState.Paused) {
					doPlayersPlayerPause(ci, player);
				}
				else if (player.getPlayState() == PlayState.Playing) {
					ci.println("Already playing.");
				}
				else {
					PlayItem currentItem = player.getCurrentItem();
					if (currentItem != null) {
						player.loadAndStartPlaying(currentItem);
					}
					else {
						ci.println("Nothing to play.");
					}
				}
			}
		}
		else if (addToQueue && args.size() == 1 && args.get(0).equals("clear")) {
			player.clearQueue();
			ci.println("Queue for player " + player.getId() + " cleared.");
		}
		else {
			String q1 = args.get(0);
			String q2 = args.size() >= 2 ? args.get(1) : null;

			List<PlayItem> results = null;
			try {
				results = CliHelper.queryForPlayableItems(q1, q2, 10);
			} catch (MorriganException e) {
				ci.println(ErrorHelper.getCauseTrace(e));
			}

			if (results == null || results.size() < 1) {
				ci.println("No results for query '"+q1+"' '"+q2+"'.");
			}
			else if (results.size() == 1) {
				if (addToQueue) {
					player.addToQueue(results.get(0));
					ci.println("Enqueued '"+results.get(0).toString()+"'.");
				}
				else {
					player.loadAndStartPlaying(results.get(0));
				}
			}
			else {
				ci.println("Multipe results for query:");
				for (PlayItem pi : results) {
					ci.println(" > " + pi.toString());
				}
			}

		}
	}

	static private void doPlayersPlayerPause (CommandInterpreter ci, IPlayerLocal player) {
		player.pausePlaying();
		ci.println("Player " + player.getId() + ": " + player.getPlayState().toString());
	}

	static private void doPlayersPlayerStop (CommandInterpreter ci, IPlayerLocal player) {
		player.stopPlaying();
		ci.println("Player " + player.getId() + ": " + player.getPlayState().toString());
	}

	static private void doPlayersPlayerNext (CommandInterpreter ci, IPlayerLocal player) {
		player.nextTrack();
		PlayItem currentItem = player.getCurrentItem();
		if (currentItem == null) {
			ci.println("Player " + player.getId() + ": " + player.getPlayState().toString());
		}
		else {
			ci.println("Player " + player.getId() + ": " + currentItem.item.getTitle());
		}
	}

	static private void doPlayersPlayerOrder (CommandInterpreter ci, IPlayerLocal player, List<String> args) {
		if (args.size() < 1) {
			ci.println("Player " + player.getId() + " order = " + player.getPlaybackOrder().toString() + ".");
			ci.print("Options:");
			for (PlaybackOrder i : PlaybackOrder.values()) {
				ci.print(" '");
				ci.print(i.toString());
				ci.print("'");
			}
			ci.println();
			return;
		}

		String arg = args.get(0);
		for (PlaybackOrder po : PlaybackOrder.values()) {
			if (po.toString().toLowerCase().contains(arg.toLowerCase())) {
				player.setPlaybackOrder(po);
				ci.println("Playback order set to '"+po.toString()+"' for player "+player.getId()+".");
				return;
			}
		}
		ci.println("Unknown playback order '"+arg+"'.");
	}

	static private void doPlayersPlayerPrintQueue (CommandInterpreter ci, IPlayerLocal player) {
		List<PlayItem> queue = player.getQueueList();

		if (queue.size() < 1) {
			ci.println("Queue for player " + player.getId() + " is empty.");
			return;
		}

		DurationData duration = player.getQueueTotalDuration();
		ci.println("Player " + player.getId() + " has " + queue.size()
				+ " items totaling " + (duration.isComplete() ? "" : " more than ")
				+ TimeHelper.formatTimeSeconds(duration.getDuration()) + " in its queue.");
		for (PlayItem pi : queue) {
			ci.println(" > " + pi.toString());
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Top-level shortcuts.
//	TODO reduce code duplication?

	static private void doPlay (CommandInterpreter ci, List<String> args) {
		if (PlayerRegister.getLocalPlayers().size() == 1) {
			IPlayerLocal player = PlayerRegister.getLocalPlayer(0);
			doPlayersPlayerPlay(ci, player, args, false);
		}
		else {
			ci.println("There is not only one player, so you need to specfy the player to use.");
		}
	}

	static private void doQueue (CommandInterpreter ci, List<String> args) {
		if (PlayerRegister.getLocalPlayers().size() == 1) {
			IPlayerLocal player = PlayerRegister.getLocalPlayer(0);
			doPlayersPlayerPlay(ci, player, args, true);
		}
		else {
			ci.println("There is not only one player, so you need to specfy the player to use.");
		}
	}

	static private void doPause (CommandInterpreter ci) {
		if (PlayerRegister.getLocalPlayers().size() == 1) {
			IPlayerLocal player = PlayerRegister.getLocalPlayer(0);
			doPlayersPlayerPause(ci, player);
		}
		else {
			ci.println("There is not only one player, so you need to specfy the player to use.");
		}
	}

	static private void doStop (CommandInterpreter ci) {
		if (PlayerRegister.getLocalPlayers().size() == 1) {
			IPlayerLocal player = PlayerRegister.getLocalPlayer(0);
			doPlayersPlayerStop(ci, player);
		}
		else {
			ci.println("There is not only one player, so you need to specfy the player to use.");
		}
	}

	static private void doNext (CommandInterpreter ci) {
		if (PlayerRegister.getLocalPlayers().size() == 1) {
			IPlayerLocal player = PlayerRegister.getLocalPlayer(0);
			doPlayersPlayerNext(ci, player);
		}
		else {
			ci.println("There is not only one player, so you need to specfy the player to use.");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
