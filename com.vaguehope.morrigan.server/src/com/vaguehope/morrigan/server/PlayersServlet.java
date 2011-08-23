package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.player.IPlayerAbstract;
import com.vaguehope.morrigan.player.IPlayerLocal;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.server.feedwriters.AbstractFeed;
import com.vaguehope.morrigan.server.feedwriters.XmlHelper;
import com.vaguehope.morrigan.util.TimeHelper;

/**
 * Valid URLs:
 * <pre>
 *  GET /players
 * 
 *  GET /players/0
 * POST /players/0 action=playpause
 * POST /players/0 action=next
 * POST /players/0 action=stop
 * POST /players/0 action=fullscreen&monitor=0
 * 
 *  GET /players/0/queue
 * POST /players/0/queue/0 action=top
 * POST /players/0/queue/0 action=up
 * POST /players/0/queue/0 action=down
 * POST /players/0/queue/0 action=bottom
 * </pre>
 */
public class PlayersServlet extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String CONTEXTPATH = "/players";
	
	public static final String PATH_QUEUE = "queue";
	
	private static final String CMD_PLAYPAUSE = "playpause";
	private static final String CMD_NEXT = "next";
	private static final String CMD_STOP = "stop";
	private static final String CMD_FULLSCREEN = "fullscreen";
	
	private static final String CMD_CLEAR = "clear";
	private static final String CMD_SHUFFLE = "shuffle";
	private static final String CMD_TOP = "top";
	private static final String CMD_UP = "up";
	private static final String CMD_REMOVE = "remove";
	private static final String CMD_DOWN = "down";
	private static final String CMD_BOTTOM = "bottom";
	
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
		try {
			String requestURI = req.getRequestURI();
			String reqPath = requestURI.startsWith(CONTEXTPATH) ? requestURI.substring(CONTEXTPATH.length()) : requestURI;
			
			if (reqPath == null || reqPath.length() < 1 || reqPath.equals(ROOTPATH)) { // POST to root.
				resp.setContentType("text/plain");
				resp.getWriter().println("Invalid POST request desu~");
			}
			else {
				String path = reqPath.startsWith(ROOTPATH) ? reqPath.substring(ROOTPATH.length()) : reqPath;
				if (path.length() > 0) {
					String[] pathParts = path.split("/");
					int playerId = Integer.parseInt(pathParts[0]);
					IPlayerAbstract player = PlayerRegister.getLocalPlayer(playerId);
					if (pathParts.length == 1) {
						postToPlayer(req, resp, player);
					}
					else if (pathParts.length >= 2 && PATH_QUEUE.equals(pathParts[1])) {
						if (pathParts.length >= 3) {
							int item = Integer.parseInt(pathParts[2]);
							postToQueue(req, resp, player, item);
						}
						else {
							postToQueue(req, resp, player);
						}
					}
					else {
						resp.setContentType("text/plain");
						resp.getWriter().println("Invalid POST request to player "+player.getId()+": '"+path+"' desu~");
					}
				}
				else {
					resp.setContentType("text/plain");
					resp.getWriter().println("Invalid POST request to '"+path+"' desu~");
				}
			}
		}
		catch (SAXException e) {
			throw new ServletException(e);
		}
	}
	
	private static void postToPlayer (HttpServletRequest req, HttpServletResponse resp, IPlayerAbstract player) throws IOException, ServletException {
		String act = req.getParameter("action");
		if (act == null) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("text/plain");
			resp.getWriter().println("HTTP Error 400 'action' parameter not set desu~");
		}
		else if (act.equals(CMD_PLAYPAUSE)) {
			player.pausePlaying();
			writeResponse(req, resp);
		}
		else if (act.equals(CMD_NEXT)) {
			player.nextTrack();
			writeResponse(req, resp);
		}
		else if (act.equals(CMD_STOP)) {
			player.stopPlaying();
			writeResponse(req, resp);
		}
		else if (act.equals(CMD_FULLSCREEN)) {
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
	
	private static void postToQueue (HttpServletRequest req, HttpServletResponse resp, IPlayerAbstract player) throws IOException, SAXException {
		String act = req.getParameter("action");
		if (act == null) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("text/plain");
			resp.getWriter().println("HTTP Error 400 'action' parameter not set desu~");
		}
		else if (act.equals(CMD_CLEAR)) {
			player.clearQueue();
			printPlayerQueue(resp, player);
		}
		else if (act.equals(CMD_SHUFFLE)) {
			player.shuffleQueue();
			printPlayerQueue(resp, player);
		}
		else {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("text/plain");
			resp.getWriter().println("HTTP Error 400 invalid 'action' parameter '"+act+"' desu~");
		}
	}
	
	private static void postToQueue (HttpServletRequest req, HttpServletResponse resp, IPlayerAbstract player, int item) throws IOException, SAXException {
		String act = req.getParameter("action");
		if (act == null) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("text/plain");
			resp.getWriter().println("HTTP Error 400 'action' parameter not set desu~");
		}
		else if (act.equals(CMD_UP) || act.equals(CMD_DOWN) || act.equals(CMD_REMOVE)) {
			PlayItem queueItem = player.getQueueItemById(item);
			if (queueItem != null) {
				if (act.equals(CMD_REMOVE)) {
					player.removeFromQueue(queueItem);
				}
				else {
					player.moveInQueue(Arrays.asList(queueItem), act.equals(CMD_DOWN));
				}
				printPlayerQueue(resp, player);
			}
			else {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.setContentType("text/plain");
				resp.getWriter().println("HTTP Error 400 item '"+item+"' not found desu~");
			}
		}
		else if (act.equals(CMD_TOP) || act.equals(CMD_BOTTOM)) {
			resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			resp.setContentType("text/plain");
			resp.getWriter().println("top/bottom not implemented desu~");
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
	
	static private void printPlayerQueue (HttpServletResponse resp, IPlayerAbstract player) throws IOException, SAXException {
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
			String trackLink = null;
			String filename;
			String filepath;
			if (currentItem != null && currentItem.item != null) {
				trackLink = URLEncoder.encode(currentItem.item.getFilepath(), "UTF-8");
				filename = currentItem.item.getTitle(); // FIXME This is a hack :s .
				filepath = currentItem.item.getFilepath();
			}
			else {
				filename = NULL;
				filepath = NULL;
			}
			
			if (trackLink != null) AbstractFeed.addLink(dw, trackLink, "track");
			
			AbstractFeed.addElement(dw, "trackfile", filepath);
			AbstractFeed.addElement(dw, "trackfilename", filename);
			AbstractFeed.addElement(dw, "playposition", p.getCurrentPosition());
			AbstractFeed.addElement(dw, "trackduration", p.getCurrentTrackDuration());
			
			if (currentItem != null && currentItem.item != null) {
				IMediaTrack item = currentItem.item;
				if (item.getHashcode() != null) AbstractFeed.addElement(dw, "trackhash", item.getHashcode().toString(16));
				AbstractFeed.addElement(dw, "trackenabled", Boolean.toString(currentItem.item.isEnabled()));
				AbstractFeed.addElement(dw, "trackmissing", Boolean.toString(currentItem.item.isMissing()));
				AbstractFeed.addElement(dw, "trackstartcount", String.valueOf(currentItem.item.getStartCount()));
				AbstractFeed.addElement(dw, "trackendcount", String.valueOf(currentItem.item.getEndCount()));
			}
			
			Map<Integer, String> mons = p.getMonitors();
			if (mons != null) {
				for (Entry<Integer, String> mon : mons.entrySet()) {
					AbstractFeed.addElement(dw, "monitor", mon.getKey() + ":" + mon.getValue());
				}
			}
		}
	}
	
	static private void printQueue (DataWriter dw, IPlayerAbstract p) throws SAXException, UnsupportedEncodingException {
		
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
			final IMediaTrackList<? extends IMediaTrack> list = playItem.list;
			final IMediaTrack mi = playItem.item;
			
			dw.startElement("entry");
			
			AbstractFeed.addElement(dw, "title", playItem.toString());
			
			String listFile = URLEncoder.encode(AbstractFeed.filenameFromPath(list.getListId()), "UTF-8");
			AbstractFeed.addLink(dw, listFile, "list", "text/xml");
			
			AbstractFeed.addElement(dw, "id", playItem.id);
			
			if (mi != null) {
				String file = URLEncoder.encode(mi.getFilepath(), "UTF-8");
				AbstractFeed.addLink(dw, file, "item");
				
				if (mi.getDateAdded() != null) {
					AbstractFeed.addElement(dw, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
				}
				if (mi.getDateLastModified() != null) {
					AbstractFeed.addElement(dw, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
				}
				AbstractFeed.addElement(dw, "type", MediaType.TRACK.getN());
				if (mi.getHashcode() != null && !BigInteger.ZERO.equals(mi.getHashcode())) AbstractFeed.addElement(dw, "hash", mi.getHashcode().toString(16));
				AbstractFeed.addElement(dw, "enabled", Boolean.toString(mi.isEnabled()));
				AbstractFeed.addElement(dw, "missing", Boolean.toString(mi.isMissing()));
				AbstractFeed.addElement(dw, "duration", mi.getDuration());
				AbstractFeed.addElement(dw, "startcount", mi.getStartCount());
				AbstractFeed.addElement(dw, "endcount", mi.getEndCount());
				if (mi.getDateLastPlayed() != null) {
					AbstractFeed.addElement(dw, "datelastplayed", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastPlayed()));
				}
			}
			
			
			dw.endElement("entry");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
