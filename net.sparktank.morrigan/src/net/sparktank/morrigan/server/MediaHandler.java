package net.sparktank.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.helpers.ErrorHelper;
import net.sparktank.morrigan.gui.model.MediaExplorerItem;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaList;
import net.sparktank.morrigan.model.MediaListFactory;
import net.sparktank.morrigan.model.library.LibraryHelper;
import net.sparktank.morrigan.model.library.MediaLibrary;
import net.sparktank.morrigan.model.playlist.MediaPlaylist;
import net.sparktank.morrigan.model.playlist.PlaylistHelper;
import net.sparktank.morrigan.player.Player;
import net.sparktank.morrigan.player.PlayerRegister;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class MediaHandler extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		System.err.println("request:t=" + target);
		
		response.getWriter().println("<h1>Media desu~</h1>");
		response.getWriter().println("<p><a href=\"/\">home</a></p>");
		
		StringBuilder sb = null;
		try {
			if (target.equals("/")) {
				sb = getMediaLists();
				
			} else {
				String r = target.substring(1);
				
				if (r.contains("/")) {
					String[] split = r.split("/");
					String type = split[0];
					String id = split[1];
					
					if (split.length > 2) {
						String param = split[2];
						if (param.equals("src")) {
							sb = getLibSrc(id);
						}
						
					} else {
						if (type.equals("library")) {
							sb = getLibrary(id);
							
						} else if (type.equals("playlist")) {
							sb = getPlaylist(id);
						}
					}
				}
				
			}
			
		} catch (Throwable t) {
			sb = new StringBuilder();
			sb.append(ErrorHelper.getStackTrace(t));
		}
		
		if (sb != null) {
			response.setContentType("text/html;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			response.getWriter().println(sb.toString());
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private StringBuilder getMediaLists () {
		StringBuilder sb = new StringBuilder();
		
		for (int n = 0; n < 2; n++) {
			ArrayList<MediaExplorerItem> items = new ArrayList<MediaExplorerItem>();
			String type = null;
			
			switch (n) {
				case 0:
					sb.append("<h2>Libraries</h2>");
					type="library";
					items.addAll(LibraryHelper.getAllLibraries());
					break;
				
				case 1:
					sb.append("<h2>Playlists</h2>");
					type="playlist";
					items.addAll(PlaylistHelper.getAllPlaylists());
					break;
				
			}
			
			List<Player> players = PlayerRegister.getPlayers();
			
			sb.append("<ul>");
			for (MediaExplorerItem i : items) {
				sb.append("<li><a href=\"/media/");
				sb.append(type);
				sb.append("/");
				String fileName = i.identifier.substring(i.identifier.lastIndexOf(File.separator) + 1);
				sb.append(fileName);
				sb.append("\">");
				sb.append(i.title);
				sb.append("</a> ");
				for (Player p : players) {
					sb.append("[<a href=\"/player/");
					sb.append(p.getId());
					sb.append("/play/");
					sb.append(fileName);
					sb.append("\">play with p");
					sb.append(p.getId());
					sb.append("</a>]");
				}
				sb.append("</li>");
			}
			sb.append("</ul>");
			
		}
		
		return sb;
	}
	
	private StringBuilder getLibrary (String id) throws MorriganException {
		StringBuilder sb = new StringBuilder();
		String f = LibraryHelper.getFullPathToLib(id);
		MediaLibrary ml = MediaListFactory.makeMediaLibrary(f);
		printMediaList(id, ml, sb);
		return sb;
	}
	
	private StringBuilder getPlaylist (String id) throws MorriganException {
		StringBuilder sb = new StringBuilder();
		String f = PlaylistHelper.getFullPathToPlaylist(id);
		MediaPlaylist ml = MediaListFactory.makeMediaPlaylist(f);
		printMediaList(id, ml, sb);
		return sb;
	}
	
	private StringBuilder getLibSrc (String id) throws MorriganException {
		StringBuilder sb = new StringBuilder();
		String f = LibraryHelper.getFullPathToLib(id);
		MediaLibrary ml = MediaListFactory.makeMediaLibrary(f);
		printLibSrc(id, ml, sb);
		return sb;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void printMediaList (String id, MediaList ml, StringBuilder sb) throws MorriganException {
		ml.read();
		List<MediaItem> mediaTracks = ml.getMediaTracks();
		
		sb.append("<h2>" + ml.getListName() + "</h2>");
		
		sb.append("<p><a href=\"/media/library/" + id + "/src\">src</a></p>");
		
		sb.append("<ul>");
		for (MediaItem i : mediaTracks) {
			// FIXME put actual track ID here.
			sb.append("<li><a href=\"/media/library/" + id + "/00000\">" + i.getTitle() + "</a></li>");
		}
		sb.append("</ul>");
	}
	
	private void printLibSrc (String id, MediaLibrary ml, StringBuilder sb) throws MorriganException {
		sb.append("<h2>" + ml.getListName() + " src</h2>");
		
		List<String> src = ml.getSources();
		sb.append("<ul>");
		for (String s : src) {
			sb.append("<li>");
			sb.append(s);
			sb.append("</li>");
		}
		sb.append("</ul>");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
