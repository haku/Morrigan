package net.sparktank.morrigan.osgiconsole;

import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDbHelper;
import net.sparktank.morrigan.model.pictures.gallery.LocalGalleryHelper;
import net.sparktank.morrigan.model.tracks.library.local.LocalLibraryHelper;
import net.sparktank.morrigan.model.tracks.library.remote.RemoteLibraryHelper;
import net.sparktank.morrigan.model.tracks.playlist.PlaylistHelper;
import net.sparktank.morrigan.player.PlayItem;
import net.sparktank.morrigan.player.Player;
import net.sparktank.morrigan.player.PlayerRegister;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

public class MorriganCommandProvider implements CommandProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getHelp() {
		return "---Morrigan---\n" +
				"\tmn media\n" +
				"\tmn players\n" +
				"\tmn player 0\n" +
				"\tmn player 0 play\n" +
				"\tmn player 0 play <item>\n" +
				"\tmn player 0 pause\n" +
				"\tmn player 0 stop\n" +
				"\tmn player 0 next\n";
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
			System.out.println("Unknown command '"+cmd+"'.");
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
		for (Player p : players) {
			System.out.print(p.getId());
			System.out.print(" ");
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
			// TODO
			String cmd = args.get(0);
			System.out.println("TODO: find and start playing '"+cmd+"'.");
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
}
