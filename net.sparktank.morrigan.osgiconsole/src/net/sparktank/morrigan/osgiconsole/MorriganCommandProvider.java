package net.sparktank.morrigan.osgiconsole;

import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDbHelper;
import net.sparktank.morrigan.model.pictures.gallery.LocalGalleryHelper;
import net.sparktank.morrigan.model.tracks.library.local.LocalLibraryHelper;
import net.sparktank.morrigan.model.tracks.library.remote.RemoteLibraryHelper;
import net.sparktank.morrigan.model.tracks.playlist.PlaylistHelper;
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
		else {
			String q1 = cmd;
			String q2 = args.size() >= 1 ? args.get(0) : null;
			
			List<PlayItem> results = null;
			try {
				results = PlayerHelper.queryForPlayableItems(q1, q2, 10);
			} catch (MorriganException e) {
				e.printStackTrace();
			}
			
			if (results == null || results.size() < 1) {
				System.out.println("No results for query '"+q1+"' '"+q2+"'.");
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
		items.addAll(LocalLibraryHelper.getAllLibraries());
		items.addAll(LocalGalleryHelper.getAllGalleries());
		items.addAll(RemoteLibraryHelper.getAllRemoteLibraries());
		items.addAll(PlaylistHelper.getAllPlaylists());
		for (MediaExplorerItem i : items) {
			System.out.println(i.type + " " + i.title);
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
		else {
			System.out.println("Unknown command '"+cmd+"'.");
		}
	}
	
	static private void doPlayersPlayerInfo (Player player) {
		System.out.print("Player ");
		System.out.print(player.getId());
		System.out.print(": ");
		System.out.print(player.getPlayState().toString());
		System.out.println();
		
		PlayItem currentItem = player.getCurrentItem();
		String item;
		if (currentItem != null && currentItem.item != null) {
			item = currentItem.item.getTitle();
		} else {
			item = "";
		}
		System.out.println("\titem=" + item);
		System.out.println("\tlist=" + player.getCurrentList());
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
