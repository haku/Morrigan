package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayItemType;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerFinder;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.server.MlistsServlet.IncludeTags;
import com.vaguehope.morrigan.server.util.FeedHelper;
import com.vaguehope.morrigan.server.util.XmlHelper;
import com.vaguehope.morrigan.transcode.Transcode;
import com.vaguehope.morrigan.util.StringHelper;
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
 * POST /players/0 action=setvolume&volume=69
 * POST /players/0 action=seek&position=123.45
 * POST /players/0 action=playbackorder&order=random
 * POST /players/0 action=transcode&transcode=audio_only
 * POST /players/0 action=fullscreen&monitor=0
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
 *
 * POST /players/auto action=playapuse
 * POST /players/auto action=next
 * </pre>
 */
public class PlayersServlet extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String REL_CONTEXTPATH = "players";
	public static final String CONTEXTPATH = "/" + REL_CONTEXTPATH;

	private static final String PLAYER_AUTO = "auto";
	public static final String PATH_QUEUE = "queue";

	private static final String CMD_PLAYPAUSE = "playpause";
	private static final String CMD_NEXT = "next";
	private static final String CMD_STOP = "stop";
	private static final String CMD_SETVOLUME = "setvolume";
	private static final String CMD_SEEK = "seek";
	private static final String CMD_PLAYBACKORDER = "playbackorder";
	private static final String CMD_TRANSCODE = "transcode";
	private static final String CMD_ADDTAG = "addtag";

	private static final String CMD_CLEAR = "clear";
	private static final String CMD_SHUFFLE = "shuffle";
	private static final String CMD_ADD_STOP_TOP = "add_stop_top";
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
	private final Config config;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public PlayersServlet (final PlayerReader playerListener, final Config config) {
		this.playerListener = playerListener;
		this.config = config;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private Player getPlayerById (final String playerId) throws UnsupportedEncodingException {
		if (StringHelper.blank(playerId)) return null;

		if (PLAYER_AUTO.equalsIgnoreCase(playerId)) {
			return PlayerFinder.guessActivePlayer(this.playerListener.getPlayers());
		}

		final Player playerById = this.playerListener.getPlayer(playerId);
		if (playerById != null) return playerById;

		for (final Player player : this.playerListener.getPlayers()) {
			if (playerId.equalsIgnoreCase(player.getName())) {
				return player;
			}
		}

		final String decodeId = URLDecoder.decode(playerId, "UTF-8");
		for (final Player player : this.playerListener.getPlayers()) {
			if (decodeId.equalsIgnoreCase(player.getName())) {
				return player;
			}
		}

		return null;
	}

	@Override
	protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			writeResponse(req, resp, null);
		}
		catch (final SAXException e) {
			throw new ServletException(e);
		}
		catch (final MorriganException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void doPost (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			final String reqPath = ServletHelper.getReqPath(req, REL_CONTEXTPATH);
			if (reqPath == null || reqPath.length() < 1 || reqPath.equals(ROOTPATH)) { // POST to root.
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid POST request desu~");
			}
			else {
				final String path = reqPath.startsWith(ROOTPATH) ? reqPath.substring(ROOTPATH.length()) : reqPath;
				if (path.length() > 0) {
					final String[] pathParts = path.split("/");
					final String playerId = pathParts[0];

					final Player player = getPlayerById(playerId);
					if (player == null) {
						ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, playerId + " not found desu~");
						return;
					}

					if (pathParts.length == 1) {
						postToPlayer(req, resp, player);
					}
					else if (pathParts.length >= 2 && PATH_QUEUE.equals(pathParts[1])) {
						if (pathParts.length >= 3) {
							final int item = Integer.parseInt(pathParts[2]);
							postToQueue(req, resp, player, item);
						}
						else {
							postToQueue(req, resp, player);
						}
					}
					else {
						ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid POST request to player " + player.getId() + ": '" + path + "' desu~");
					}
				}
				else {
					ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid POST request to '" + path + "' desu~");
				}
			}
		}
		catch (final SAXException e) {
			throw new ServletException(e);
		}
		catch (final MorriganException e) {
			throw new ServletException(e);
		}
	}

	private void postToPlayer (final HttpServletRequest req, final HttpServletResponse resp, final Player player) throws IOException, SAXException, MorriganException {
		final String act = req.getParameter("action");
		if (act == null) {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'action' parameter not set desu~");
		}
		else if (act.equals(CMD_PLAYPAUSE)) {
			player.pausePlaying();
			writeResponse(req, resp, player);
		}
		else if (act.equals(CMD_NEXT)) {
			player.nextTrack();
			writeResponse(req, resp, player);
		}
		else if (act.equals(CMD_STOP)) {
			player.stopPlaying();
			writeResponse(req, resp, player);
		}
		else if (act.equals(CMD_SETVOLUME)) {
			final String volumeRaw = req.getParameter("volume");
			if (volumeRaw != null && volumeRaw.length() > 0) {
				final int volume = Integer.parseInt(volumeRaw); // TODO handle ex?
				if (volumeRaw.startsWith("+") || volumeRaw.startsWith("-")) {
					player.setVolume(player.getVoume() + volume);
				}
				else {
					player.setVolume(volume);
				}
				writeResponse(req, resp, player);
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'volume' parameter not set desu~");
			}
		}
		else if (act.equals(CMD_SEEK)) {
			final String positionRaw = req.getParameter("position");
			if (positionRaw != null && positionRaw.length() > 0) {
				final double position = Double.parseDouble(positionRaw); // TODO handle ex?
				final int duration = player.getCurrentTrackDuration();
				player.seekTo(position / duration); // WTF was I thinking when I wrote this API?
				writeResponse(req, resp, player);
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'position' parameter not set desu~");
			}
		}
		else if (act.equals(CMD_PLAYBACKORDER)) {
			final String orderRaw = req.getParameter("order");
			if (orderRaw != null && orderRaw.length() > 0) {
				final PlaybackOrder order = PlaybackOrder.parsePlaybackOrderByName(orderRaw);
				player.setPlaybackOrder(order);
				writeResponse(req, resp, player);
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'order' parameter not set desu~");
			}
		}
		else if (act.equals(CMD_TRANSCODE)) {
			final String transcodeRaw = req.getParameter("transcode");
			if (transcodeRaw != null) {
				final Transcode transcode = Transcode.parse(transcodeRaw);
				player.setTranscode(transcode);
				writeResponse(req, resp, player);
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'order' parameter not set desu~");
			}
		}
		else if (act.equals(CMD_ADDTAG)) {
			final String tag = req.getParameter("tag");
			if (tag != null && tag.length() > 0) {
				final PlayItem currentItem = player.getCurrentItem();
				final IMediaTrack item = currentItem != null ? currentItem.getTrack() : null;
				final IMediaTrackList<? extends IMediaTrack> list = currentItem != null ? currentItem.getList() : null;
				if (item != null && list != null) {
					list.addTag(item, tag, MediaTagType.MANUAL, (MediaTagClassification) null);
					writeResponse(req, resp, player);
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
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "invalid 'action' parameter '" + act + "' desu~");
		}
	}

	private static void postToQueue (final HttpServletRequest req, final HttpServletResponse resp, final Player player) throws IOException, SAXException {
		final String act = req.getParameter("action");
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
		else if (act.equals(CMD_ADD_STOP_TOP)) {
			player.getQueue().addToQueueTop(player.getQueue().makeMetaItem(PlayItemType.STOP));
			printPlayerQueue(resp, player);
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "invalid 'action' parameter '" + act + "' desu~");
		}
	}

	private static void postToQueue (final HttpServletRequest req, final HttpServletResponse resp, final Player player, final int item) throws IOException, SAXException {
		final String act = req.getParameter("action");
		if (act == null) {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'action' parameter not set desu~");
		}
		else if (act.equals(CMD_UP) || act.equals(CMD_DOWN) || act.equals(CMD_REMOVE) || act.equals(CMD_TOP) || act.equals(CMD_BOTTOM)) {
			final PlayItem queueItem = player.getQueue().getQueueItemById(item);
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
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "item '" + item + "' not found desu~");
			}
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "invalid 'action' parameter '" + act + "' desu~");
		}
	}

	private void writeResponse (final HttpServletRequest req, final HttpServletResponse resp, final Player foundPlayer) throws IOException, SAXException, MorriganException {
		final String reqPath = ServletHelper.getReqPath(req, REL_CONTEXTPATH);
		if (reqPath == null || reqPath.length() < 1 || reqPath.equals(ROOTPATH)) {
			printPlayersList(resp);
		}
		else {
			final String path = reqPath.substring(ROOTPATH.length()); // Expecting path = '0' or '0/queue'.
			if (path.length() > 0) {
				final String[] pathParts = path.split("/");
				if (pathParts.length >= 1) {
					final String playerId = pathParts[0];
					final Player player = foundPlayer != null ? foundPlayer : getPlayerById(playerId);
					if (player == null) {
						ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, playerId + " not found desu~");
						return;
					}

					if (pathParts.length >= 2) {
						final String subPath = pathParts[1];
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
		final DataWriter dw = FeedHelper.startFeed(resp.getWriter());

		FeedHelper.addElement(dw, "title", "Morrigan players desu~");
		FeedHelper.addLink(dw, REL_CONTEXTPATH, "self", "text/xml");

		final Collection<Player> players = this.playerListener.getPlayers();
		for (final Player p : players) {
			dw.startElement("entry");
			printPlayer(dw, p, 0);
			dw.endElement("entry");
		}

		FeedHelper.endFeed(dw);
	}

	private void printPlayer (final HttpServletResponse resp, final Player player) throws IOException, SAXException, MorriganException {
		resp.setContentType("text/xml;charset=utf-8");
		final DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "player");

		printPlayer(dw, player, 1);

		FeedHelper.endDocument(dw, "player");
	}

	private static void printPlayerQueue (final HttpServletResponse resp, final Player player) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		final DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "queue");

		printQueue(dw, player);

		FeedHelper.endDocument(dw, "queue");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void printPlayer (final DataWriter dw, final Player p, final int detailLevel) throws SAXException, MorriganException, IOException {
		if (detailLevel < 0 || detailLevel > 1) throw new IllegalArgumentException("detailLevel must be 0 or 1, not " + detailLevel + ".");

		final String listTitle;
		final String listId;
		final String listUrl;
		final String listView;
		final IMediaTrackList<? extends IMediaTrack> currentList = p.getCurrentList();
		if (currentList != null) {
			listTitle = currentList.getListName();
			listId = currentList.getListId();
			listUrl = MlistsServlet.REL_CONTEXTPATH + "/" + currentList.getType() + "/" + URLEncoder.encode(FeedHelper.filenameFromPath(currentList.getListId()), "UTF-8");
			listView = StringHelper.trimToEmpty(currentList.getSearchTerm());
		}
		else {
			listTitle = NULL;
			listId = NULL;
			listUrl = null;
			listView = "";
		}

		final PlayItem currentItem = p.getCurrentItem();
		final String trackTitle = (currentItem != null && currentItem.hasTrack()) ? currentItem.getTrack().getTitle() : "(empty)";
		final Integer volume = p.getVoume();
		final Integer volumeMaxValue = p.getVoumeMaxValue();

		final long queueVersion = p.getQueue().getVersion();
		final int queueLength = p.getQueue().getQueueList().size();
		final DurationData queueDuration = p.getQueue().getQueueTotalDuration();

		FeedHelper.addElement(dw, "title", "p" + p.getId() + ":" + p.getPlayState().toString() + ":" + trackTitle);
		final String selfUrl = REL_CONTEXTPATH + "/" + p.getId();
		FeedHelper.addLink(dw, selfUrl, "self", "text/xml");

		FeedHelper.addElement(dw, "playerid", p.getId());
		FeedHelper.addElement(dw, "playername", p.getName());
		FeedHelper.addElement(dw, "playstate", p.getPlayState().getN());
		if (volume != null) FeedHelper.addElement(dw, "volume", volume);
		if (volumeMaxValue != null) FeedHelper.addElement(dw, "volumemaxvalue", volumeMaxValue);
		FeedHelper.addElement(dw, "playorderid", p.getPlaybackOrder().name());
		FeedHelper.addElement(dw, "playordertitle", p.getPlaybackOrder().toString());
		FeedHelper.addElement(dw, "transcode", p.getTranscode().getSymbolicName());
		FeedHelper.addElement(dw, "transcodetitle", p.getTranscode().toString());
		FeedHelper.addElement(dw, "queueversion", queueVersion);
		FeedHelper.addElement(dw, "queuelength", queueLength);
		FeedHelper.addElement(dw, "queueduration", queueDuration.getDuration());
		FeedHelper.addLink(dw, selfUrl + "/" + PATH_QUEUE, "queue", "text/xml");
		FeedHelper.addElement(dw, "listtitle", listTitle);
		FeedHelper.addElement(dw, "listid", listId);
		if (listUrl != null) FeedHelper.addLink(dw, listUrl, "list", "text/xml");
		FeedHelper.addElement(dw, "listview", listView);
		FeedHelper.addElement(dw, "tracktitle", trackTitle);

		if (detailLevel == 1) {
			final String trackLink;
			final String filename;
			final String filepath;
			if (currentItem != null && currentItem.hasTrack()) {
				trackLink = MlistsServlet.fileLink(currentItem.getTrack());
				filename = currentItem.getTrack().getTitle(); // FIXME This is a hack :s .
				filepath = StringHelper.notBlank(currentItem.getTrack().getFilepath()) ? currentItem.getTrack().getFilepath() : NULL;
			}
			else {
				trackLink = null;
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
				FeedHelper.addElement(dw, "trackfilesize", track.getFileSize());
				if (track.getMd5() != null) FeedHelper.addElement(dw, "trackhash", track.getMd5().toString(16));
				FeedHelper.addElement(dw, "trackenabled", Boolean.toString(track.isEnabled()));
				FeedHelper.addElement(dw, "trackmissing", Boolean.toString(track.isMissing()));
				FeedHelper.addElement(dw, "trackstartcount", String.valueOf(track.getStartCount()));
				FeedHelper.addElement(dw, "trackendcount", String.valueOf(track.getEndCount()));

				if (currentList != null) {
					final List<MediaTag> tags = currentList.getTags(track);
					if (tags != null) {
						for (final MediaTag tag : tags) {
							FeedHelper.addElement(dw, "tracktag", tag.getTag(), new String[][] {
								{"t", String.valueOf(tag.getType().getIndex())},
								{"c", tag.getClassification() == null ? "" : tag.getClassification().getClassification()}
							});
						}
					}

					dw.startElement("track");
					MlistsServlet.fillInMediaItem(dw, currentList, track, IncludeTags.YES, null, this.config);
					dw.endElement("track");
				}
			}
		}
	}

	private static void printQueue (final DataWriter dw, final Player p) throws SAXException, UnsupportedEncodingException {
		FeedHelper.addLink(dw, REL_CONTEXTPATH + "/" + p.getId() + "/" + PATH_QUEUE, "self", "text/xml");
		FeedHelper.addLink(dw, REL_CONTEXTPATH + "/" + p.getId(), "player", "text/xml");

		FeedHelper.addElement(dw, "queueversion", p.getQueue().getVersion());
		FeedHelper.addElement(dw, "queuelength", p.getQueue().getQueueList().size());

		final DurationData queueDuration = p.getQueue().getQueueTotalDuration();
		final String queueDurationString = (queueDuration.isComplete() ? "" : "more than ") +
				TimeHelper.formatTimeSeconds(queueDuration.getDuration());
		FeedHelper.addElement(dw, "queueduration", queueDurationString); // FIXME make parsasble.

		for (final PlayItem playItem : p.getQueue().getQueueList()) {
			dw.startElement("entry");
			FeedHelper.addElement(dw, "title", playItem.toString());
			FeedHelper.addElement(dw, "id", playItem.getId());

			if (playItem.hasList()) {
				final String listFile = URLEncoder.encode(FeedHelper.filenameFromPath(playItem.getList().getListId()), "UTF-8");
				FeedHelper.addLink(dw, listFile, "list", "text/xml");
			}

			if (playItem.hasTrack()) {
				final IMediaTrack mi = playItem.getTrack();
				FeedHelper.addLink(dw, MlistsServlet.fileLink(mi), "item");

				if (mi.getDateAdded() != null) {
					FeedHelper.addElement(dw, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
				}
				if (mi.getDateLastModified() != null) {
					FeedHelper.addElement(dw, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
				}
				FeedHelper.addElement(dw, "type", MediaType.TRACK.getN());
				FeedHelper.addElement(dw, "filesize", mi.getFileSize());
				if (mi.getMd5() != null && !BigInteger.ZERO.equals(mi.getMd5())) FeedHelper.addElement(dw, "hash", mi.getMd5().toString(16));
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
