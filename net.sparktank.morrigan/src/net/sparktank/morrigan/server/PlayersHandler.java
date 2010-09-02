package net.sparktank.morrigan.server;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.ErrorHelper;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDb;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDbHelper;
import net.sparktank.morrigan.model.tracks.library.local.LocalLibraryHelper;
import net.sparktank.morrigan.model.tracks.library.local.LocalMediaLibrary;
import net.sparktank.morrigan.model.tracks.playlist.MediaPlaylist;
import net.sparktank.morrigan.model.tracks.playlist.PlaylistHelper;
import net.sparktank.morrigan.player.PlayItem;
import net.sparktank.morrigan.player.Player;
import net.sparktank.morrigan.player.PlayerRegister;
import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class PlayersHandler extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		System.err.println("request:t=" + target);
		
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		response.getWriter().println("<h1>Players desu~</h1>");
		response.getWriter().println("<p><a href=\"/\">home</a></p>");
		
		StringBuilder sb = new StringBuilder();
		try {
			if (target.equals("/")) {
				sb = getPlayers();
			}
			else {
				String id = target.substring(1);
				if (id.contains("/")) {
					String[] split = id.split("/");
					id = split[0];
					String action = split[1];
					
					if (split.length >= 3) {
						doAction(Integer.parseInt(id), action, split[2]);
					} else {
						doAction(Integer.parseInt(id), action, null);
					}
				}
				sb = getPlayer(Integer.parseInt(id));
			}
		}
		catch (Throwable t) {
			sb = new StringBuilder();
			sb.append(ErrorHelper.getStackTrace(t));
		}
		
		response.getWriter().println(sb.toString());
	}
	
	static private StringBuilder getPlayers () {
		StringBuilder sb = new StringBuilder();
		sb.append("<h2>All players</h2>");
		
		List<Player> players = PlayerRegister.getPlayers();
		sb.append("<ul>");
		for (Player p : players) {
			sb.append("<li><a href=\"/player/"+p.getId()+"\"> p"+p.getId());
			sb.append(" " + p.getPlayState().toString()+ ": ");
			PlayItem currentItem = p.getCurrentItem();
			if (currentItem != null && currentItem.item != null) {
				sb.append(currentItem.item.getTitle());
			}
			sb.append("</a></li>");
		}
		sb.append("</ul>");
		
		return sb;
	}
	
	static private StringBuilder getPlayer (int n) {
		StringBuilder sb = new StringBuilder();
		sb.append("<h2><a href=\"/player/"+n+"\">Player "+n+"</a></h2>");
		sb.append("<p style=\"font-size:1.5em;text-align:center;\"><a href=\"/player/"+n+"/playpause\">play / pause</a> | <a href=\"/player/"+n+"/next\">next</a></p>");
		
		Player player = PlayerRegister.getPlayer(n);
		sb.append("<ul>");
		sb.append("<li>state="+player.getPlayState().toString()+"</li>");
		PlayItem currentItem = player.getCurrentItem();
		String item;
		if (currentItem != null && currentItem.item != null) {
			item = currentItem.item.getTitle();
		} else {
			item = "";
		}
		sb.append("<li>item="+item+"</li>");
		sb.append("<li>list="+player.getCurrentList()+"</li>");
		sb.append("</ul>");
		
		return sb;
	}
	
	static private void doAction (int id, String action, String param) throws MorriganException {
		System.err.println("[doAction] id=" + id + ", action=" + action + ", param=" + param);
		Player player = PlayerRegister.getPlayer(id);
		
		String a = action.toLowerCase();
		if (a.equals("playpause")) {
			player.pausePlaying();
		}
		else if (a.equals("next")) {
			player.nextTrack();
		}
		else if (a.equals("play")) {
			doPlay(player, param);
		}
	}
	
	static private void doPlay (Player player, String param) throws MorriganException {
		if (LocalMixedMediaDbHelper.isMmdbFile(param)) {
			String f = LocalMixedMediaDbHelper.getFullPathToMmdb(param);
			LocalMixedMediaDb mmdb;
			try {
				mmdb = LocalMixedMediaDb.LOCAL_MMDB_FACTORY.manufacture(f);
			} catch (DbException e) {
				throw new MorriganException(e);
			}
			mmdb.read();
			player.loadAndStartPlaying(mmdb);
		}
		else if (LocalLibraryHelper.isLibFile(param)) {
			String f = LocalLibraryHelper.getFullPathToLib(param);
			LocalMediaLibrary ml;
			try {
				ml = LocalMediaLibrary.FACTORY.manufacture(f);
			} catch (DbException e) {
				throw new MorriganException(e);
			}
			ml.read();
			player.loadAndStartPlaying(ml);
		}
		else if (PlaylistHelper.isPlFile(param)) {
			String f = PlaylistHelper.getFullPathToPlaylist(param);
			MediaPlaylist ml = MediaPlaylist.FACTORY.manufacture(f);
			ml.read();
			player.loadAndStartPlaying(ml);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
