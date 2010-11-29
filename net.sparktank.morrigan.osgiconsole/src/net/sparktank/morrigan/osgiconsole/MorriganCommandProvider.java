package net.sparktank.morrigan.osgiconsole;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.media.DurationData;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMediaTrackList;
import net.sparktank.morrigan.model.media.internal.LocalMixedMediaDb;
import net.sparktank.morrigan.model.media.internal.LocalMixedMediaDbHelper;
import net.sparktank.morrigan.player.IPlayerLocal;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;
import net.sparktank.morrigan.player.PlayItem;
import net.sparktank.morrigan.player.PlayerHelper;
import net.sparktank.morrigan.player.PlayerRegister;
import net.sparktank.morrigan.server.HeadlessHelper;
import net.sparktank.morrigan.server.model.RemoteMixedMediaDb;
import net.sparktank.morrigan.server.model.RemoteMixedMediaDbHelper;
import net.sparktank.morrigan.util.TimeHelper;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

public class MorriganCommandProvider implements CommandProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getHelp() {
		return "---Morrigan---\n" +
				"\tmn media\n" +
				"\tmn media create <dbname>\n" +
				"\tmn media add <dir> <q1>\n" +
				"\tmn media scan <q1>\n" +
				"\tmn media <q1>\n" +
				"\tmn media <q1> <q2>\n" +
				"\tmn players\n" +
				"\tmn player 0\n" +
				"\tmn player 0 play\n" +
				"\tmn player 0 play <q1>\n" +
				"\tmn player 0 play <q1> <q2>\n" +
				"\tmn player 0 queue\n" +
				"\tmn player 0 queue <q1>\n" +
				"\tmn player 0 queue <q1> <q2>\n" +
				"\tmn player 0 queue clear\n" +
				"\tmn player 0 pause\n" +
				"\tmn player 0 stop\n" +
				"\tmn player 0 next\n" +
				"\tmn player 0 order <order>\n" +
				"\tmn play\n" +
				"\tmn play <q1>\n" +
				"\tmn play <q1> <q2>\n" +
				"\tmn queue\n" +
				"\tmn queue <q1>\n" +
				"\tmn queue <q1> <q2>\n" +
				"\tmn pause\n" +
				"\tmn stop\n" +
				"\tmn next\n" +
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
		if (cmd.equals("m") || cmd.equals("media")) {
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
			doPause(ci, args);
		}
		else if (cmd.equals("s") || cmd.equals("stop")) {
			doStop(ci, args);
		}
		else if (cmd.equals("n") || cmd.equals("next")) {
			doNext(ci, args);
		}
		else {
			ci.println("Unknown command '"+cmd+"'.");
		}
		
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
		else if (cmd.equals("s") || cmd.equals("u") || cmd.equals("scan") || cmd.equals("update")) {
			doMediaScan(ci, args);
		}
		else if (cmd.equals("c") || cmd.equals("create")) {
			doMediaCreate(ci, args);
		}
		else if (cmd.equals("a") || cmd.equals("add")) {
			doMediaAdd(ci, args);
		}
		else {
			String q1 = cmd;
			String q2 = args.size() >= 1 ? args.get(0) : null;
			
			List<PlayItem> results = null;
			try {
				results = PlayerHelper.queryForPlayableItems(q1, q2, 10);
			} catch (MorriganException e) {
				e.printStackTrace();
				return;
			}
			
			if (results == null || results.size() < 1) {
				ci.println("No results for query '"+q1+"' '"+q2+"'.");
			}
			else if (results.size() == 1) {
				IMediaTrackList<? extends IMediaTrack> list = results.get(0).list;
				ci.println("Query match: " + list);
				if (results.get(0).list instanceof LocalMixedMediaDb) {
					LocalMixedMediaDb mmdb = (LocalMixedMediaDb) list;
					List<String> sources;
					try {
						sources = mmdb.getSources();
					} catch (MorriganException e) {
						e.printStackTrace();
						return;
					}
					for (String s : sources) {
						ci.println(" src > " + s);
					}
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
		List<MediaExplorerItem> items = new LinkedList<MediaExplorerItem>();
		items.addAll(LocalMixedMediaDbHelper.getAllMmdb());
		items.addAll(RemoteMixedMediaDbHelper.getAllRemoteMmdb());
		for (MediaExplorerItem i : items) {
			ci.println(i.type + " " + i.title);
		}
	}
	
	static private void doMediaCreate (CommandInterpreter ci, List<String> args) {
		if (args.size() >= 1) {
			String name = args.get(0);
			try {
				ILocalMixedMediaDb mmdb = LocalMixedMediaDbHelper.createMmdb(name);
				ci.println("Created MMDB '"+mmdb.getListName()+"'.");
			}
			catch (MorriganException e) {
				e.printStackTrace();
			}
		}
		else {
			ci.println("You must specify a name for the new DB.");
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
			results = PlayerHelper.queryForPlayableItems(q1, null, 2);
		}
		catch (MorriganException e) {
			e.printStackTrace();
			return;
		}
		
		if (results == null || results.size() != 1) {
			ci.println("Query '"+q1+"' did not return only one result.");
		}
		else {
			IMediaTrackList<? extends IMediaTrack> list = results.get(0).list;
			if (list instanceof LocalMixedMediaDb) {
				LocalMixedMediaDb mmdb = (LocalMixedMediaDb) list;
				try {
					mmdb.addSource(dir.getAbsolutePath());
				} catch (MorriganException e) {
					e.printStackTrace();
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
				results = PlayerHelper.queryForPlayableItems(q1, null, 2);
			}
			catch (MorriganException e) {
				e.printStackTrace();
				return;
			}
			
			if (results == null || results.size() != 1) {
				ci.println("Query '"+q1+"' did not return only one result.");
			}
			else {
				IMediaTrackList<? extends IMediaTrack> list = results.get(0).list;
				if (list instanceof LocalMixedMediaDb) {
					HeadlessHelper.scheduleMmdbScan((LocalMixedMediaDb) list);
				}
				else if (list instanceof RemoteMixedMediaDb) {
					HeadlessHelper.scheduleRemoteMmdbScan((RemoteMixedMediaDb) list);
				}
				else {
					ci.println("Unable to schedule scan for item '"+list.getListName()+"'.");
				}
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
		List<IPlayerLocal> players = PlayerRegister.getLocalPlayers();
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
				results = PlayerHelper.queryForPlayableItems(q1, q2, 10);
			} catch (MorriganException e) {
				e.printStackTrace();
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
			ci.println("Order mode parameter not specifed.");
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
	
	static private void doPause (CommandInterpreter ci, List<String> args) {
		if (PlayerRegister.getLocalPlayers().size() == 1) {
			IPlayerLocal player = PlayerRegister.getLocalPlayer(0);
			doPlayersPlayerPause(ci, player);
		}
		else {
			ci.println("There is not only one player, so you need to specfy the player to use.");
		}
	}
	
	static private void doStop (CommandInterpreter ci, List<String> args) {
		if (PlayerRegister.getLocalPlayers().size() == 1) {
			IPlayerLocal player = PlayerRegister.getLocalPlayer(0);
			doPlayersPlayerStop(ci, player);
		}
		else {
			ci.println("There is not only one player, so you need to specfy the player to use.");
		}
	}
	
	static private void doNext (CommandInterpreter ci, List<String> args) {
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
