package net.sparktank.morrigan.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sparktank.morrigan.model.media.DurationData;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMediaTrackList;
import net.sparktank.morrigan.player.IPlayerAbstract;
import net.sparktank.morrigan.player.IPlayerLocal;
import net.sparktank.morrigan.player.PlayItem;
import net.sparktank.morrigan.player.PlayerRegister;
import net.sparktank.morrigan.server.feedwriters.AbstractFeed;
import net.sparktank.morrigan.util.TimeHelper;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;

public class PlayersServlet extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String CONTEXTPATH = "/players";
	
	public static final String PATH_QUEUE = "queue";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final long serialVersionUID = -6463380542721345844L;
	
	private static final String ROOTPATH = "/";
	private static final String NULL = "null";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		writeResponse(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		int n = Integer.parseInt(req.getPathInfo().substring(ROOTPATH.length()));
		IPlayerAbstract player = PlayerRegister.getLocalPlayer(n);
		
		String act = req.getParameter("action");
		if (act == null) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("text/plain");
			resp.getWriter().println("HTTP Error 400 'action' parameter not set desu~");
		}
		else if (act.equals("playpause")) {
			player.pausePlaying();
			writeResponse(req, resp);
		}
		else if (act.equals("next")) {
			player.nextTrack();
			writeResponse(req, resp);
		}
		else if (act.equals("stop")) {
			player.stopPlaying();
			writeResponse(req, resp);
		}
		else if (act.equals("fullscreen")) {
			String monitorString = req.getParameter("monitor");
			if (monitorString != null && monitorString.length() > 0) {
				int monitorId = Integer.parseInt(monitorString);
				player.goFullscreen(monitorId);
				writeResponse(req, resp);
			}
			else {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.setContentType("text/plain");
				resp.getWriter().println("HTTP Error 400 'fullscreen' parameter not set desu~");
			}
		}
		else {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("text/plain");
			resp.getWriter().println("HTTP Error 400 invalid 'action' parameter '"+act+"' desu~");
		}
	}
	
	private static void writeResponse (HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		String requestURI = req.getRequestURI();
		String reqPath = requestURI.startsWith(CONTEXTPATH) ? requestURI.substring(CONTEXTPATH.length()) : requestURI;
		
		if (reqPath == null || reqPath.length() < 1 || reqPath.equals(ROOTPATH)) {
			try {
				printPlayersList(resp);
			} catch (SAXException e) {
				throw new ServletException(e);
			}
		}
		else {
			String path = reqPath.substring(ROOTPATH.length()); // Expecting path = '0' or '0/queue'.
			if (path.length() > 0) {
				String[] pathParts = path.split("/");
				if (pathParts.length >= 1) {
					String playerNumberRaw = pathParts[0];
					try {
						int playerNumber = Integer.parseInt(playerNumberRaw);
						IPlayerLocal player;
						try {
							player = PlayerRegister.getLocalPlayer(playerNumber);
						}
						catch (IllegalArgumentException e) {
							resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
							resp.setContentType("text/plain");
							resp.getWriter().println("HTTP Error 404 player " + playerNumber + " not found desu~");
							return;
						}
						
						if (pathParts.length >= 2) {
							String subPath = pathParts[1];
							if (subPath.equals(PATH_QUEUE)) {
								try {
									printPlayerQueue(resp, player);
								}
								catch (SAXException e) {
									throw new ServletException(e);
								}
							}
							else {
								resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
								resp.setContentType("text/plain");
								resp.getWriter().println("HTTP Error 404 " + subPath + " not found desu~");
							}
						}
						else {
							try {
								printPlayer(resp, player);
							}
							catch (SAXException e) {
								throw new ServletException(e);
							}
						}
						
					}
					catch (NumberFormatException e) {
						resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
						resp.setContentType("text/plain");
						resp.getWriter().println("HTTP Error 404 not found '" + reqPath + "' desu~ (could not parse '"+playerNumberRaw+"' as a player.)");
					}
				}
			}
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void printPlayersList (HttpServletResponse resp) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = AbstractFeed.startFeed(resp.getWriter());
		
		AbstractFeed.addElement(dw, "title", "Morrigan players desu~");
		AbstractFeed.addLink(dw, CONTEXTPATH, "self", "text/xml");
		
		Collection<IPlayerLocal> players = PlayerRegister.getLocalPlayers();
		for (IPlayerLocal p : players) {
			dw.startElement("entry");
			printPlayer(dw, p, 0);
			dw.endElement("entry");
		}
		
		AbstractFeed.endFeed(dw);
	}
	
	static private void printPlayer (HttpServletResponse resp, IPlayerLocal player) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = AbstractFeed.startDocument(resp.getWriter(), "player");
		
		printPlayer(dw, player, 1);
		
		AbstractFeed.endDocument(dw, "player");
	}
	
	static private void printPlayerQueue (HttpServletResponse resp, IPlayerLocal player) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = AbstractFeed.startDocument(resp.getWriter(), "queue");
		
		printQueue(dw, player);
		
		AbstractFeed.endDocument(dw, "queue");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void printPlayer (DataWriter dw, IPlayerAbstract p, int detailLevel) throws SAXException, UnsupportedEncodingException {
		if (detailLevel < 0 || detailLevel > 1) throw new IllegalArgumentException("detailLevel must be 0 or 1, not "+detailLevel+".");
		
		String listTitle;
		String listId;
		String listUrl;
		IMediaTrackList<? extends IMediaTrack> currentList = p.getCurrentList();
		if (currentList != null) {
			listTitle = currentList.getListName();
			listId = currentList.getListId();
			listUrl = MlistsServlet.CONTEXTPATH + "/" + currentList.getType() + "/" + URLEncoder.encode(AbstractFeed.filenameFromPath(currentList.getListId()), "UTF-8");
		}
		else {
			listTitle = NULL;
			listId = NULL;
			listUrl = null;
		}
		
		String title;
		PlayItem currentItem = p.getCurrentItem();
		if (currentItem != null && currentItem.item != null) {
			title = currentItem.item.getTitle();
		}
		else {
			title = p.getName();
		}
		
		int queueLength = p.getQueueList().size();
		DurationData queueDuration = p.getQueueTotalDuration();
		
		AbstractFeed.addElement(dw, "title", "p" + p.getId() + ":" + p.getPlayState().toString() + ":" + title);
		String selfUrl = CONTEXTPATH + "/" + p.getId();
		AbstractFeed.addLink(dw, selfUrl, "self", "text/xml");
		
		AbstractFeed.addElement(dw, "playerid", p.getId());
		AbstractFeed.addElement(dw, "playstate", p.getPlayState().getN());
		AbstractFeed.addElement(dw, "playorder", p.getPlaybackOrder().getN());
		AbstractFeed.addElement(dw, "queuelength", queueLength);
		AbstractFeed.addElement(dw, "queueduration", queueDuration.getDuration());
		AbstractFeed.addLink(dw, selfUrl + "/" + PATH_QUEUE, "queue", "text/xml");
		AbstractFeed.addElement(dw, "listtitle", listTitle);
		AbstractFeed.addElement(dw, "listid", listId);
		if (listUrl != null) AbstractFeed.addLink(dw, listUrl, "list", "text/xml");
		AbstractFeed.addElement(dw, "tracktitle", title);
		
		if (detailLevel == 1) {
			String filepath;
			if (currentItem != null && currentItem.item != null) {
				filepath = currentItem.item.getFilepath();
			}
			else {
				filepath = NULL;
			}
			
			AbstractFeed.addElement(dw, "playposition", p.getCurrentPosition());
			AbstractFeed.addElement(dw, "trackfile", filepath);
			AbstractFeed.addElement(dw, "trackduration", p.getCurrentTrackDuration());
			
			Map<Integer, String> mons = p.getMonitors();
			if (mons != null) {
				for (Entry<Integer, String> mon : mons.entrySet()) {
					AbstractFeed.addElement(dw, "monitor", mon.getKey() + ":" + mon.getValue());
				}
			}
		}
	}
	
	static private void printQueue (DataWriter dw, IPlayerAbstract p) throws SAXException {
		
		AbstractFeed.addLink(dw, CONTEXTPATH + "/" + p.getId() + "/" + PATH_QUEUE, "self", "text/xml");
		AbstractFeed.addLink(dw, CONTEXTPATH + "/" + p.getId(), "player", "text/xml");
		
		int queueLength = p.getQueueList().size();
		DurationData queueDuration = p.getQueueTotalDuration();
		String queueDurationString = (queueDuration.isComplete() ? "" : "more than ") +
				TimeHelper.formatTimeSeconds(queueDuration.getDuration());
		
		AbstractFeed.addElement(dw, "queuelength", queueLength);
		AbstractFeed.addElement(dw, "queueduration", queueDurationString); // FIXME make parsasble.
		
		List<PlayItem> queueList = p.getQueueList();
		for (PlayItem playItem : queueList) {
			dw.startElement("entry");
			
			AbstractFeed.addElement(dw, "title", playItem.toString());
			
			IMediaTrackList<? extends IMediaTrack> list = playItem.list;
			String listFile;
			try {
				listFile = URLEncoder.encode(AbstractFeed.filenameFromPath(list.getListId()), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			
			String pathToSelf = CONTEXTPATH + "/" + list.getType() + "/" + listFile;
			AbstractFeed.addLink(dw, pathToSelf, "list", "text/xml");
			
			IMediaTrack item = playItem.item;
			if (item != null) {
				String file;
				try {
					file = URLEncoder.encode(item.getFilepath(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				
				StringBuilder sb = new StringBuilder();
				sb.append(MlistsServlet.CONTEXTPATH);
				sb.append("/");
				sb.append(list.getType());
				sb.append("/");
				sb.append(listFile);
				sb.append("/");
				sb.append(MlistsServlet.PATH_ITEM);
				sb.append("/");
				sb.append(file);
				AbstractFeed.addLink(dw, sb.toString(), "item");
			}
			
			dw.endElement("entry");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
