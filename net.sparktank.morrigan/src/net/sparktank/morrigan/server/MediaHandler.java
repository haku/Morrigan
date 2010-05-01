package net.sparktank.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sparktank.morrigan.gui.model.MediaExplorerItem;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaListFactory;
import net.sparktank.morrigan.model.library.LibraryHelper;
import net.sparktank.morrigan.model.library.MediaLibrary;
import net.sparktank.morrigan.model.playlist.PlaylistHelper;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class MediaHandler extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		StringBuilder sb = null;
		
		if (target.equals("/")) {
			sb = getMediaLists();
			
		} else {
			String d = target.substring(1,target.indexOf("/", 2));
			String id = target.substring(d.length() + 2);
			if (d.equals("library")) {
				sb = getLibrary(id);
				
			} else if (d.equals("playlist")) {
				
			}
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
		ArrayList<MediaExplorerItem> items = new ArrayList<MediaExplorerItem>();
		
		sb.append("<h1>Media desu~</h1>");
		
		sb.append("<h2>Libraries</h2>");
		items.addAll(LibraryHelper.getAllLibraries());
		sb.append("<ul>");
		for (MediaExplorerItem i : items) {
			sb.append("<li><a href=\"/media/library/" + i.identifier.substring(i.identifier.lastIndexOf(File.separator)+1) + "\">" + i.title + "</a></li>");
		}
		sb.append("</ul>");
		
		sb.append("<h2>Playlists</h2>");
		items.addAll(PlaylistHelper.getAllPlaylists());
		sb.append("<ul>");
		for (MediaExplorerItem i : items) {
			sb.append("<li><a href=\"/media/playlist/" + i.identifier.substring(i.identifier.lastIndexOf(File.separator)+1) + "\">" + i.title + "</a></li>");
		}
		sb.append("</ul>");
		
		return sb;
	}
	
	private StringBuilder getLibrary (String id) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("<h1>"+id+"</h1>");
			
			String f = LibraryHelper.getFullPathToLib(id);
			MediaLibrary ml = MediaListFactory.makeMediaLibrary(f);
			ml.read();
			List<MediaItem> mediaTracks = ml.getMediaTracks();
			
			sb.append("<ul>");
			for (MediaItem i : mediaTracks) {
				sb.append("<li><a href=\"/media/library/" + id + "/00000\">" + i.getTitle() + "</a></li>");
			}
			sb.append("</ul>");
			
			return sb;
			
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
