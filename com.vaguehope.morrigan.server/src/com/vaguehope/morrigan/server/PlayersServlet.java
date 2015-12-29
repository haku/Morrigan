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
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.player.OrderHelper;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.server.util.FeedHelper;
import com.vaguehope.morrigan.server.util.XmlHelper;
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
 * POST /players/0 action=addtag&tag=foo
 *
 *  GET /players/0/queue
 * POST /players/0/queue action=clear
 * POST /players/0/queue action=shuffle
 * POST /players/0/queue/0 action=top
 * POST /players/0/queue/0 action=up
 * POST /players/0/queue/0 action=remove
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
	private static final String CMD_PLAYBACKORDER = "playbackorder";
	private static final String CMD_FULLSCREEN = "fullscreen";
	private static final String CMD_ADDTAG = "addtag";

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

	private final PlayerReader playerListener;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public PlayersServlet (final PlayerReader playerListener) {
		this.playerListener = playerListener;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			writeResponse(req, resp);
		}
		catch (SAXException e) {
			throw new ServletException(e);
		}
		catch (MorriganException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			String requestURI = req.getRequestURI();
			String reqPath = requestURI.startsWith(CONTEXTPATH) ? requestURI.substring(CONTEXTPATH.length()) : requestURI;

			if (reqPath == null || reqPath.length() < 1 || reqPath.equals(ROOTPATH)) { // POST to root.
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid POST request desu~");
			}
			else {
				String path = reqPath.startsWith(ROOTPATH) ? reqPath.substring(ROOTPATH.length()) : reqPath;
				if (path.length() > 0) {
					String[] pathParts = path.split("/");
					String playerId = pathParts[0];
					Player player = this.playerListener.getPlayer(playerId);
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
						ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid POST request to player "+player.getId()+": '"+path+"' desu~");
					}
				}
				else {
					ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid POST request to '"+path+"' desu~");
				}
			}
		}
		catch (SAXException e) {
			throw new ServletException(e);
		}
		catch (MorriganException e) {
			throw new ServletException(e);
		}
	}

	private void postToPlayer (final HttpServletRequest req, final HttpServletResponse resp, final Player player) throws IOException, SAXException, MorriganException {
		String act = req.getParameter("action");
		if (act == null) {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'action' parameter not set desu~");
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
		else if (act.equals(CMD_PLAYBACKORDER)) {
			final String orderRaw = req.getParameter("order");
			if (orderRaw != null && orderRaw.length() > 0) {
				final PlaybackOrder order = OrderHelper.parsePlaybackOrderByName(orderRaw);
				player.setPlaybackOrder(order);
				writeResponse(req, resp);
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'order' parameter not set desu~");
			}
		}
		else if (act.equals(CMD_FULLSCREEN)) {
			String monitorString = req.getParameter("monitor");
			if (monitorString != null && monitorString.length() > 0) {
				int monitorId = Integer.parseInt(monitorString);
				player.goFullscreen(monitorId);
				writeResponse(req, resp);
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'fullscreen' parameter not set desu~");
			}
		}
		else if (act.equals(CMD_ADDTAG)) {
			String tag = req.getParameter("tag");
			if (tag != null && tag.length() > 0) {
				PlayItem currentItem = player.getCurrentItem();
				final IMediaTrack item = currentItem != null ? currentItem.getTrack() : null;
				final IMediaTrackList<? extends IMediaTrack> list = currentItem != null ? currentItem.getList() : null;
				if (item != null && list != null) {
					list.addTag(item, tag, MediaTagType.MANUAL, (MediaTagClassification)null);
					writeResponse(req, resp);
				}
				else {
					ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "no list or item to add tag to desu~");
				}
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'tag' parameter not set desu~");
			}
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "invalid 'action' parameter '"+act+"' desu~");
		}
	}

	private static void postToQueue (final HttpServletRequest req, final HttpServletResponse resp, final Player player) throws IOException, SAXException {
		String act = req.getParameter("action");
		if (act == null) {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'action' parameter not set desu~");
		}
		else if (act.equals(CMD_CLEAR)) {
			player.getQueue().clearQueue();
			printPlayerQueue(resp, player);
		}
		else if (act.equals(CMD_SHUFFLE)) {
			player.getQueue().shuffleQueue();
			printPlayerQueue(resp, player);
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "invalid 'action' parameter '"+act+"' desu~");
		}
	}

	private static void postToQueue (final HttpServletRequest req, final HttpServletResponse resp, final Player player, final int item) throws IOException, SAXException {
		String act = req.getParameter("action");
		if (act == null) {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'action' parameter not set desu~");
		}
		else if (act.equals(CMD_UP) || act.equals(CMD_DOWN) || act.equals(CMD_REMOVE) || act.equals(CMD_TOP) || act.equals(CMD_BOTTOM)) {
			PlayItem queueItem = player.getQueue().getQueueItemById(item);
			if (queueItem != null) {
				if (act.equals(CMD_REMOVE)) {
					player.getQueue().removeFromQueue(queueItem);
				}
				else if (act.equals(CMD_TOP) || act.equals(CMD_BOTTOM)) {
					player.getQueue().moveInQueueEnd(Arrays.asList(queueItem), act.equals(CMD_BOTTOM));
				}
				else if (act.equals(CMD_UP) || act.equals(CMD_DOWN)) {
					player.getQueue().moveInQueue(Arrays.asList(queueItem), act.equals(CMD_DOWN));
				}
				else {
					throw new IllegalStateException("Out of cheese desu~.");
				}
				printPlayerQueue(resp, player);
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "item '"+item+"' not found desu~");
			}
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "invalid 'action' parameter '"+act+"' desu~");
		}
	}

	private void writeResponse (final HttpServletRequest req, final HttpServletResponse resp) throws IOException, SAXException, MorriganException {
		String requestURI = req.getRequestURI();
		String reqPath = requestURI.startsWith(CONTEXTPATH) ? requestURI.substring(CONTEXTPATH.length()) : requestURI;

		if (reqPath == null || reqPath.length() < 1 || reqPath.equals(ROOTPATH)) {
			printPlayersList(resp);
		}
		else {
			String path = reqPath.substring(ROOTPATH.length()); // Expecting path = '0' or '0/queue'.
			if (path.length() > 0) {
				String[] pathParts = path.split("/");
				if (pathParts.length >= 1) {
					String playerId = pathParts[0];
					Player player = this.playerListener.getPlayer(playerId);
					if (player == null) {
						ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, playerId + " not found desu~");
						return;
					}

					if (pathParts.length >= 2) {
						String subPath = pathParts[1];
						if (subPath.equals(PATH_QUEUE)) {
							printPlayerQueue(resp, player);
						}
						else {
							ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "'" + subPath + "' not found desu~");
						}
					}
					else {
						printPlayer(resp, player);
					}
				}
			}

		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void printPlayersList (final HttpServletResponse resp) throws IOException, SAXException, MorriganException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = FeedHelper.startFeed(resp.getWriter());

		FeedHelper.addElement(dw, "title", "Morrigan players desu~");
		FeedHelper.addLink(dw, CONTEXTPATH, "self", "text/xml");

		Collection<Player> players = this.playerListener.getPlayers();
		for (Player p : players) {
			dw.startElement("entry");
			printPlayer(dw, p, 0);
			dw.endElement("entry");
		}

		FeedHelper.endFeed(dw);
	}

	private static void printPlayer (final HttpServletResponse resp, final Player player) throws IOException, SAXException, MorriganException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "player");

		printPlayer(dw, player, 1);

		FeedHelper.endDocument(dw, "player");
	}

	private static void printPlayerQueue (final HttpServletResponse resp, final Player player) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "queue");

		printQueue(dw, player);

		FeedHelper.endDocument(dw, "queue");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static void printPlayer (final DataWriter dw, final Player p, final int detailLevel) throws SAXException, UnsupportedEncodingException, MorriganException {
		if (detailLevel < 0 || detailLevel > 1) throw new IllegalArgumentException("detailLevel must be 0 or 1, not "+detailLevel+".");

		String listTitle;
		String listId;
		String listUrl;
		IMediaTrackList<? extends IMediaTrack> currentList = p.getCurrentList();
		if (currentList != null) {
			listTitle = currentList.getListName();
			listId = currentList.getListId();
			listUrl = MlistsServlet.CONTEXTPATH + "/" + currentList.getType() + "/" + URLEncoder.encode(FeedHelper.filenameFromPath(currentList.getListId()), "UTF-8");
		}
		else {
			listTitle = NULL;
			listId = NULL;
			listUrl = null;
		}

		PlayItem currentItem = p.getCurrentItem();
		String trackTitle = (currentItem != null && currentItem.hasTrack()) ? currentItem.getTrack().getTitle() : "(empty)";

		int queueLength = p.getQueue().getQueueList().size();
		DurationData queueDuration = p.getQueue().getQueueTotalDuration();

		FeedHelper.addElement(dw, "title", "p" + p.getId() + ":" + p.getPlayState().toString() + ":" + trackTitle);
		String selfUrl = CONTEXTPATH + "/" + p.getId();
		FeedHelper.addLink(dw, selfUrl, "self", "text/xml");

		FeedHelper.addElement(dw, "playerid", p.getId());
		FeedHelper.addElement(dw, "playername", p.getName());
		FeedHelper.addElement(dw, "playstate", p.getPlayState().getN());
		FeedHelper.addElement(dw, "playorder", p.getPlaybackOrder().getN());
		FeedHelper.addElement(dw, "playordertitle", p.getPlaybackOrder().toString());
		FeedHelper.addElement(dw, "queuelength", queueLength);
		FeedHelper.addElement(dw, "queueduration", queueDuration.getDuration());
		FeedHelper.addLink(dw, selfUrl + "/" + PATH_QUEUE, "queue", "text/xml");
		FeedHelper.addElement(dw, "listtitle", listTitle);
		FeedHelper.addElement(dw, "listid", listId);
		if (listUrl != null) FeedHelper.addLink(dw, listUrl, "list", "text/xml");
		FeedHelper.addElement(dw, "tracktitle", trackTitle);

		if (detailLevel == 1) {
			String trackLink = null;
			String filename;
			String filepath;
			if (currentItem != null && currentItem.hasTrack()) {
				trackLink = URLEncoder.encode(currentItem.getTrack().getFilepath(), "UTF-8");
				filename = currentItem.getTrack().getTitle(); // FIXME This is a hack :s .
				filepath = currentItem.getTrack().getFilepath();
			}
			else {
				filename = NULL;
				filepath = NULL;
			}

			if (trackLink != null) FeedHelper.addLink(dw, trackLink, "track");

			FeedHelper.addElement(dw, "trackfile", filepath);
			FeedHelper.addElement(dw, "trackfilename", filename);
			FeedHelper.addElement(dw, "playposition", p.getCurrentPosition());
			FeedHelper.addElement(dw, "trackduration", p.getCurrentTrackDuration());

			if (currentItem != null && currentItem.hasTrack()) {
				final IMediaTrack track = currentItem.getTrack();
				if (track.getHashcode() != null) FeedHelper.addElement(dw, "trackhash", track.getHashcode().toString(16));
				FeedHelper.addElement(dw, "trackenabled", Boolean.toString(track.isEnabled()));
				FeedHelper.addElement(dw, "trackmissing", Boolean.toString(track.isMissing()));
				FeedHelper.addElement(dw, "trackstartcount", String.valueOf(track.getStartCount()));
				FeedHelper.addElement(dw, "trackendcount", String.valueOf(track.getEndCount()));

				if (currentList != null) {
					List<MediaTag> tags = currentList.getTags(track);
					if (tags != null) {
						for (MediaTag tag : tags) {
							FeedHelper.addElement(dw, "tracktag", tag.getTag(), new String[][] {
								{"t", String.valueOf(tag.getType().getIndex())},
								{"c", tag.getClassification() == null ? "" : tag.getClassification().getClassification()}
							});
						}
					}
				}
			}

			Map<Integer, String> mons = p.getMonitors();
			if (mons != null) {
				for (Entry<Integer, String> mon : mons.entrySet()) {
					FeedHelper.addElement(dw, "monitor", mon.getKey() + ":" + mon.getValue());
				}
			}
		}
	}

	private static void printQueue (final DataWriter dw, final Player p) throws SAXException, UnsupportedEncodingException {

		FeedHelper.addLink(dw, CONTEXTPATH + "/" + p.getId() + "/" + PATH_QUEUE, "self", "text/xml");
		FeedHelper.addLink(dw, CONTEXTPATH + "/" + p.getId(), "player", "text/xml");

		int queueLength = p.getQueue().getQueueList().size();
		DurationData queueDuration = p.getQueue().getQueueTotalDuration();
		String queueDurationString = (queueDuration.isComplete() ? "" : "more than ") +
				TimeHelper.formatTimeSeconds(queueDuration.getDuration());

		FeedHelper.addElement(dw, "queuelength", queueLength);
		FeedHelper.addElement(dw, "queueduration", queueDurationString); // FIXME make parsasble.

		List<PlayItem> queueList = p.getQueue().getQueueList();
		for (PlayItem playItem : queueList) {
			final IMediaTrackList<? extends IMediaTrack> list = playItem.getList();
			final IMediaTrack mi = playItem.getTrack();

			dw.startElement("entry");

			FeedHelper.addElement(dw, "title", playItem.toString());

			String listFile = URLEncoder.encode(FeedHelper.filenameFromPath(list.getListId()), "UTF-8");
			FeedHelper.addLink(dw, listFile, "list", "text/xml");

			FeedHelper.addElement(dw, "id", playItem.getId());

			if (mi != null) {
				String file = URLEncoder.encode(mi.getFilepath(), "UTF-8");
				FeedHelper.addLink(dw, file, "item");

				if (mi.getDateAdded() != null) {
					FeedHelper.addElement(dw, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
				}
				if (mi.getDateLastModified() != null) {
					FeedHelper.addElement(dw, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
				}
				FeedHelper.addElement(dw, "type", MediaType.TRACK.getN());
				if (mi.getHashcode() != null && !BigInteger.ZERO.equals(mi.getHashcode())) FeedHelper.addElement(dw, "hash", mi.getHashcode().toString(16));
				FeedHelper.addElement(dw, "enabled", Boolean.toString(mi.isEnabled()));
				FeedHelper.addElement(dw, "missing", Boolean.toString(mi.isMissing()));
				FeedHelper.addElement(dw, "duration", mi.getDuration());
				FeedHelper.addElement(dw, "startcount", mi.getStartCount());
				FeedHelper.addElement(dw, "endcount", mi.getEndCount());
				if (mi.getDateLastPlayed() != null) {
					FeedHelper.addElement(dw, "datelastplayed", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastPlayed()));
				}
			}


			dw.endElement("entry");
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
