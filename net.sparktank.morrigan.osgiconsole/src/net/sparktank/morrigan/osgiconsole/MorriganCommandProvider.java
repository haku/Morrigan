package net.sparktank.morrigan.osgiconsole;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.media.HeadlessHelper;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDb;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDbHelper;
import net.sparktank.morrigan.model.media.impl.RemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.impl.RemoteMixedMediaDbHelper;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrackList;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;
import net.sparktank.morrigan.player.PlayItem;
import net.sparktank.morrigan.player.Player;
import net.sparktank.morrigan.player.PlayerHelper;
import net.sparktank.morrigan.player.PlayerRegister;

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
				"\tmn player 0 pause\n" +
				"\tmn player 0 stop\n" +
				"\tmn player 0 next\n" +
				"\tmn player 0 order <order>\n" +
				"\tmn play\n" +
				"\tmn play <q1>\n" +
				"\tmn play <q1> <q2>\n" +
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
			System.out.println("No method specified.");
			return;
		}
		
		String cmd = args.remove(0);
		if (cmd.equals("media")) {
			doMedia(args);
		}
		else if (cmd.equals("players") || cmd.equals("player")) {
			doPlayers(args);
		}
		else if (cmd.equals("play")) {
			doPlay(args);
		}
		else if (cmd.equals("pause")) {
			doPause(args);
		}
		else if (cmd.equals("stop")) {
			doStop(args);
		}
		else if (cmd.equals("next")) {
			doNext(args);
		}
		else {
			System.out.println("Unknown command '"+cmd+"'.");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void doMedia (List<String> args) {
		if (args.size() < 1) {
			doMediaList();
			return;
		}
		
		String cmd = args.remove(0);
		if (cmd.equals("list")) {
			doMediaList();
		}
		else if (cmd.equals("scan") || cmd.equals("update")) {
			doMediaScan(args);
		}
		else if (cmd.equals("create")) {
			doMediaCreate(args);
		}
		else if (cmd.equals("add")) {
			doMediaAdd(args);
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
				System.out.println("No results for query '"+q1+"' '"+q2+"'.");
			}
			else if (results.size() == 1) {
				IMediaTrackList<? extends IMediaTrack> list = results.get(0).list;
				System.out.println("Query match: " + list);
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
						System.out.println(" src > " + s);
					}
				}
			}
			else {
				System.out.println("Results for query:");
				for (PlayItem pi : results) {
					System.out.println(" > " + pi.toString());
				}
			}
		}
	}
	
	static private void doMediaList () {
		List<MediaExplorerItem> items = new LinkedList<MediaExplorerItem>();
		items.addAll(LocalMixedMediaDbHelper.getAllMmdb());
		items.addAll(RemoteMixedMediaDbHelper.getAllRemoteMmdb());
		for (MediaExplorerItem i : items) {
			System.out.println(i.type + " " + i.title);
		}
	}
	
	static private void doMediaCreate (List<String> args) {
		if (args.size() >= 1) {
			String name = args.get(0);
			try {
				LocalMixedMediaDb mmdb = LocalMixedMediaDbHelper.createMmdb(name);
				System.out.println("Created MMDB '"+mmdb.getListName()+"'.");
			}
			catch (MorriganException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("You must specify a name for the new DB.");
		}
	}
	
	static private void doMediaAdd (List<String> args) {
		if (args.size() < 2) {
			System.out.println("Not enough arguments.");
			return;
		}
		
		String dirArg = args.get(0);
		String q1 = args.get(1);
		
		File dir = new File(dirArg);
		if (!dir.exists()) {
			System.out.println("Directory '"+dir.getAbsolutePath()+"' not found.");
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
			System.out.println("Query '"+q1+"' did not return only one result.");
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
				System.out.println("You can not edit the sources for a remote library.");
			}
			else {
				System.out.println("Unable to add to the item type of '"+list.getListName()+"'.");
			}
		}
	}
	
	static private void doMediaScan (List<String> args) {
		if (args.size() < 1) {
			System.out.println("No query parameter.");
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
				System.out.println("Query '"+q1+"' did not return only one result.");
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
					System.out.println("Unable to schedule scan for item '"+list.getListName()+"'.");
				}
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void doPlayers (List<String> args) {
		if (args.size() < 1) {
			doPlayersList();
			return;
		}
		
		String cmd = args.remove(0);
		try {
			int playerId = Integer.parseInt(cmd);
			Player player = PlayerRegister.getPlayer(playerId);
			doPlayersPlayer(player, args);
		}
		catch (NumberFormatException e) {
			// If we only have one player, assume the next param is a cmd.
			if (PlayerRegister.getPlayers().size() == 1) {
				Player player = PlayerRegister.getPlayer(0);
				args.add(0, cmd);
				doPlayersPlayer(player, args);
			}
			else {
				System.out.println("Unknown player ID '"+cmd+"'.");
			}
		}
	}
	
	static private void doPlayersList() {
		List<Player> players = PlayerRegister.getPlayers();
		System.out.println("id\tplayer");
		for (Player p : players) {
			System.out.print(p.getId());
			System.out.print("\t");
			System.out.print(p.getPlayState());
			
			PlayItem currentItem = p.getCurrentItem();
			if (currentItem != null && currentItem.item != null) {
				System.out.print(": ");
				System.out.print(currentItem.item.getTitle());
			}
			
			System.out.println();
		}
	}
	
	static private void doPlayersPlayer (Player player, List<String> args) {
		if (args.size() < 1) {
			doPlayersPlayerInfo(player);
			return;
		}
		
		String cmd = args.remove(0);
		if (cmd.equals("play")) {
			doPlayersPlayerPlay(player, args);
		}
		else if (cmd.equals("pause")) {
			doPlayersPlayerPause(player);
		}
		else if (cmd.equals("stop")) {
			doPlayersPlayerStop(player);
		}
		else if (cmd.equals("next")) {
			doPlayersPlayerNext(player);
		}
		else if (cmd.equals("order")) {
			doPlayersPlayerOrder(player, args);
		}
		else {
			System.out.println("Unknown command '"+cmd+"'.");
		}
	}
	
	static private void doPlayersPlayerInfo (Player player) {
		System.out.print("Player ");
		System.out.print(player.getId());
		System.out.print(": ");
		System.out.print(player.getPlayState().toString());
		System.out.print(" (");
		System.out.print(player.getPlaybackOrder().toString());
		System.out.print(")");
		System.out.println();
		
		PlayItem currentItem = player.getCurrentItem();
		String item = (currentItem != null && currentItem.item != null) ? currentItem.item.getTitle() : "";
		System.out.println("\titem=" + item);
		
		IMediaTrackList<? extends IMediaTrack> currentList = player.getCurrentList();
		String list = currentList != null ? currentList.getListName() : "";
		System.out.println("\tlist=" + list);
	}
	
	static private void doPlayersPlayerPlay (Player player, List<String> args) {
		if (args.size() < 1) {
			if (player.getPlayState() == PlayState.Paused) {
				doPlayersPlayerPause(player);
			}
			else if (player.getPlayState() == PlayState.Playing) {
				System.out.println("Already playing.");
			}
			else {
				PlayItem currentItem = player.getCurrentItem();
				if (currentItem != null) {
					player.loadAndStartPlaying(currentItem);
				}
				else {
					System.out.println("Nothing to play.");
				}
			}
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
				System.out.println("No results for query '"+q1+"' '"+q2+"'.");
			}
			else if (results.size() == 1) {
				player.loadAndStartPlaying(results.get(0));
			}
			else {
				System.out.println("Multipe results for query:");
				for (PlayItem pi : results) {
					System.out.println(" > " + pi.toString());
				}
			}
			
		}
	}
	
	static private void doPlayersPlayerPause (Player player) {
		player.pausePlaying();
		System.out.println("Player " + player.getId() + ": " + player.getPlayState().toString());
	}
	
	static private void doPlayersPlayerStop (Player player) {
		player.stopPlaying();
		System.out.println("Player " + player.getId() + ": " + player.getPlayState().toString());
	}
	
	static private void doPlayersPlayerNext (Player player) {
		player.nextTrack();
		PlayItem currentItem = player.getCurrentItem();
		if (currentItem == null) {
			System.out.println("Player " + player.getId() + ": " + player.getPlayState().toString());
		}
		else {
			System.out.println("Player " + player.getId() + ": " + currentItem.item.getTitle());
		}
	}
	
	static private void doPlayersPlayerOrder (Player player, List<String> args) {
		if (args.size() < 1) {
			System.out.println("Order mode parameter not specifed.");
			return;
		}
		
		String arg = args.get(0);
		for (PlaybackOrder po : PlaybackOrder.values()) {
			if (po.toString().toLowerCase().contains(arg.toLowerCase())) {
				player.setPlaybackOrder(po);
				System.out.println("Playback order set to '"+po.toString()+"' for player "+player.getId()+".");
				return;
			}
		}
		System.out.println("Unknown playback order '"+arg+"'.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Top-level shortcuts.
//	TODO reduce code duplication?
	
	static private void doPlay (List<String> args) {
		if (PlayerRegister.getPlayers().size() == 1) {
			Player player = PlayerRegister.getPlayer(0);
			doPlayersPlayerPlay(player, args);
		}
		else {
			System.out.println("There is not only one player, so you need to specfy the player to use.");
		}
	}
	
	static private void doPause (List<String> args) {
		if (PlayerRegister.getPlayers().size() == 1) {
			Player player = PlayerRegister.getPlayer(0);
			doPlayersPlayerPause(player);
		}
		else {
			System.out.println("There is not only one player, so you need to specfy the player to use.");
		}
	}
	
	static private void doStop (List<String> args) {
		if (PlayerRegister.getPlayers().size() == 1) {
			Player player = PlayerRegister.getPlayer(0);
			doPlayersPlayerStop(player);
		}
		else {
			System.out.println("There is not only one player, so you need to specfy the player to use.");
		}
	}
	
	static private void doNext (List<String> args) {
		if (PlayerRegister.getPlayers().size() == 1) {
			Player player = PlayerRegister.getPlayer(0);
			doPlayersPlayerNext(player);
		}
		else {
			System.out.println("There is not only one player, so you need to specfy the player to use.");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
