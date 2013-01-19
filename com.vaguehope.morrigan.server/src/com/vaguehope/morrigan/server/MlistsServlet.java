package com.vaguehope.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDbHelper;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbFactory;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbHelper;
import com.vaguehope.morrigan.server.util.FeedHelper;
import com.vaguehope.morrigan.server.util.XmlHelper;
import com.vaguehope.sqlitewrapper.DbException;

/**
 * Valid URLs:
 *
 * <pre>
 *  GET /mlists
 *
 *  GET /mlists/LOCALMMDB/example.local.db3
 *  GET /mlists/LOCALMMDB/example.local.db3/src
 * POST /mlists/LOCALMMDB/example.local.db3 action=play&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3 action=queue&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3 action=scan
 *
 *  GET /mlists/LOCALMMDB/example.local.db3/items
 *  GET /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3 action=play&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3 action=queue&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3 action=addtag&tag=foo
 *
 *  GET /mlists/LOCALMMDB/example.local.db3/albums
 * POST /mlists/LOCALMMDB/example.local.db3/albums/somealbum action=play&playerid=0 // TODO
 * POST /mlists/LOCALMMDB/example.local.db3/albums/somealbum action=queue&playerid=0 // TODO
 *
 *  GET /mlists/LOCALMMDB/example.local.db3/query/example
 * </pre>
 */
public class MlistsServlet extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String CONTEXTPATH = "/mlists";

	public static final String PATH_SRC = "src";
	public static final String PATH_ITEMS = "items";
	public static final String PATH_ALBUMS = "albums";
	public static final String PATH_QUERY = "query";

	public static final String CMD_NEWMMDB = "newmmdb";
	public static final String CMD_SCAN = "scan";
	public static final String CMD_PLAY = "play";
	public static final String CMD_QUEUE = "queue";
	public static final String CMD_ADDTAG = "addtag";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final long serialVersionUID = 2754601524882233866L;

	private static final String ROOTPATH = "/";
	private static final int MAX_RESULTS = 150;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final PlayerReader playerListener;
	private final MediaFactory mediaFactory;
	private final AsyncActions asyncActions;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MlistsServlet (PlayerReader playerListener, MediaFactory mediaFactory, AsyncActions asyncActions) {
		this.playerListener = playerListener;
		this.mediaFactory = mediaFactory;
		this.asyncActions = asyncActions;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected void doGet (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			processRequest(Verb.GET, req, resp, null);
		}
		catch (DbException e) {
			throw new ServletException(e);
		}
		catch (SAXException e) {
			throw new ServletException(e);
		}
		catch (MorriganException e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void doPost (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			String act = req.getParameter("action");
			if (act == null) {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP Error 400 'action' parameter not set desu~");
			}
			else {
				processRequest(Verb.POST, req, resp, act);
			}
		}
		catch (DbException e) {
			throw new ServletException(e);
		}
		catch (SAXException e) {
			throw new ServletException(e);
		}
		catch (MorriganException e) {
			throw new ServletException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static enum Verb {
		GET, POST
	}

	/**
	 * Param action will not be null when verb==POST.
	 */
	private void processRequest (Verb verb, HttpServletRequest req, HttpServletResponse resp, String action) throws IOException, DbException, SAXException, MorriganException {
		String requestURI = req.getRequestURI();
		String reqPath = requestURI.startsWith(CONTEXTPATH) ? requestURI.substring(CONTEXTPATH.length()) : requestURI;

		if (reqPath == null || reqPath.length() < 1 || reqPath.equals(ROOTPATH)) {
			if (verb == Verb.POST) {
				postToRoot(resp, action);
			}
			else {
				printMlistList(resp);
			}
		}
		else {
			String path = reqPath.startsWith(ROOTPATH) ? reqPath.substring(ROOTPATH.length()) : reqPath;
			if (path.length() > 0) {
				String[] pathParts = path.split("/");
				if (pathParts.length >= 2) {
					String type = pathParts[0];
					if (type.equals(ILocalMixedMediaDb.TYPE) || type.equals(IRemoteMixedMediaDb.TYPE)) {
						IMixedMediaDb mmdb;
						if (type.equals(ILocalMixedMediaDb.TYPE)) {
							String f = LocalMixedMediaDbHelper.getFullPathToMmdb(pathParts[1]);
							mmdb = this.mediaFactory.getLocalMixedMediaDb(f);
						}
						else if (type.equals(IRemoteMixedMediaDb.TYPE)) {
							String f = RemoteMixedMediaDbHelper.getFullPathToMmdb(pathParts[1]);
							mmdb = RemoteMixedMediaDbFactory.getExisting(f);
						}
						else {
							throw new IllegalArgumentException("Out of cheese desu~.  Please reinstall universe and reboot desu~.");
						}

						String subPath = pathParts.length >= 3 ? pathParts[2] : null;
						String afterSubPath = pathParts.length >= 4 ? pathParts[3] : null;
						if (verb == Verb.POST) {
							postToMmdb(req, resp, action, mmdb, subPath, afterSubPath);
						}
						else {
							getToMmdb(resp, mmdb, subPath, afterSubPath);
						}
					}
					else {
						ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Unknown type '" + type + "' desu~.");
					}
				}
				else {
					ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request '" + path + "' desu~");
				}
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request '" + path + "' desu~");
			}
		}
	}

	private static void postToRoot (HttpServletResponse resp, String action) throws IOException {
		if (action.equals(CMD_NEWMMDB)) {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "TODO implement create new MMDB cmd desu~");
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP error 400 '" + action + "' is not a valid action parameter desu~");
		}
	}

	private void postToMmdb (HttpServletRequest req, HttpServletResponse resp, String action, IMixedMediaDb mmdb, String path, String afterPath) throws IOException, MorriganException, DbException {
		if (path != null && path.equals(PATH_ITEMS) && afterPath != null && afterPath.length() > 0) {
			String filepath = URLDecoder.decode(afterPath, "UTF-8");
			if (mmdb.hasFile(filepath)) {
				IMixedMediaItem item = mmdb.getByFile(filepath);
				if (item != null) {
					postToMmdbItem(req, resp, action, mmdb, item);
				}
				else {
					ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "Failed to retrieve file '" + filepath + "' from MMDB when it should have been there desu~.");
				}
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "HTTP error 404 file '" + filepath + "' not found in MMDB '" + mmdb.getListName() + "' desu~");
			}
		}
		else {
			postToMmdb(req, resp, action, mmdb);
		}
	}

	private void postToMmdb (HttpServletRequest req, HttpServletResponse resp, String action, IMixedMediaDb mmdb) throws IOException {
		if (action.equals(CMD_PLAY) || action.equals(CMD_QUEUE)) {
			Player player = parsePlayer(req, resp);
			if (player != null) { // parsePlayer() will write the error msg.
				resp.setContentType("text/plain");
				if (action.equals(CMD_PLAY)) {
					player.loadAndStartPlaying(mmdb);
					resp.getWriter().println("MMDB playing desu~");
				}
				else if (action.equals(CMD_QUEUE)) {
					player.addToQueue(new PlayItem(mmdb, null));
					resp.getWriter().println("MMDB added to queue desu~");
				}
				else {
					throw new IllegalArgumentException("The world has exploded desu~.");
				}
			}
		}
		else if (action.equals(CMD_SCAN)) {
			this.asyncActions.scheduleMmdbScan(mmdb);
			resp.setContentType("text/plain");
			resp.getWriter().println("Scan scheduled desu~");
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP error 400 '" + action + "' is not a valid action parameter desu~");
		}
	}

	private void postToMmdbItem (HttpServletRequest req, HttpServletResponse resp, String action, IMixedMediaDb mmdb, IMixedMediaItem item) throws IOException, MorriganException {
		if (action.equals(CMD_PLAY) || action.equals(CMD_QUEUE)) {
			Player player = parsePlayer(req, resp);
			if (player != null) { // parsePlayer() will write the error msg.
				resp.setContentType("text/plain");
				if (action.equals(CMD_PLAY)) {
					player.loadAndStartPlaying(mmdb, item);
					resp.getWriter().println("Item playing desu~");
				}
				else if (action.equals(CMD_QUEUE)) {
					player.addToQueue(new PlayItem(mmdb, item));
					resp.getWriter().println("Item added to queue desu~");
				}
				else {
					throw new IllegalArgumentException("The world has exploded desu~.");
				}
			}
		}
		else if (action.equals(CMD_ADDTAG)) {
			String tag = req.getParameter("tag");
			if (tag != null && tag.length() > 0) {
				mmdb.addTag(item, tag, MediaTagType.MANUAL, (MediaTagClassification) null);
				resp.setContentType("text/plain");
				resp.getWriter().println("Tag '" + tag + "' added desu~");
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'tag' parameter not set desu~");
			}
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP error 400 '" + action + "' is not a valid action parameter desu~");
		}
	}

	private Player parsePlayer (HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String playerIdS = req.getParameter("playerid");
		if (playerIdS == null) {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP error 400 'playerId' parameter not set desu~");
			return null;
		}
		int playerId = Integer.parseInt(playerIdS);
		return this.playerListener.getPlayer(playerId);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void printMlistList (HttpServletResponse resp) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = FeedHelper.startFeed(resp.getWriter());

		FeedHelper.addElement(dw, "title", "Morrigan media lists desu~");
		FeedHelper.addLink(dw, CONTEXTPATH, "self", "text/xml");

		Collection<Player> players = this.playerListener.getPlayers();

		// TODO merge 2 loops.

		for (MediaListReference listRef : this.mediaFactory.getAllLocalMixedMediaDbs()) {
			dw.startElement("entry");
			printMlistShort(dw, listRef, players);
			dw.endElement("entry");
		}

		for (MediaListReference listRef : RemoteMixedMediaDbHelper.getAllRemoteMmdb()) {
			dw.startElement("entry");
			printMlistShort(dw, listRef, players);
			dw.endElement("entry");
		}

		FeedHelper.endFeed(dw);
	}

	private static void getToMmdb (HttpServletResponse resp, IMixedMediaDb mmdb, String path, String afterPath) throws IOException, SAXException, MorriganException, DbException {
		if (path == null) {
			printMlistLong(resp, mmdb, false, false);
		}
		else if (path.equals(PATH_ITEMS)) {
			if (afterPath != null && afterPath.length() > 0) {
				// Request to fetch media file.
				String filepath = URLDecoder.decode(afterPath, "UTF-8");
				if (mmdb.hasFile(filepath)) {
					IMixedMediaItem item = mmdb.getByFile(filepath);
					boolean asDownload = item.getMediaType() == MediaType.TRACK;
					File file = new File(filepath);
					if (file.exists()) {
						ServletHelper.returnFile(file, resp, asDownload);
					}
					else {
						String name = asDownload ? item.getTitle() : null;
						ServletHelper.prepForReturnFile(name, 0, resp); // TODO pass through length?
						mmdb.copyItemFile(item, resp.getOutputStream());
						resp.flushBuffer();
					}
				}
				else {
					ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "HTTP error 404 '" + filepath + "' is not in '" + mmdb.getListId() + "' desu~");
				}
			}
			else {
				printMlistLong(resp, mmdb, false, true);
			}
		}
		else if (path.equals(PATH_SRC)) {
			printMlistLong(resp, mmdb, true, false);
		}
		else if (path.equals(PATH_ALBUMS)) {
			printAlbums(resp, mmdb);
		}
		else if (path.equals(PATH_QUERY) && afterPath != null && afterPath.length() > 0) {
			String query = URLDecoder.decode(afterPath, "UTF-8");
			printMlistLong(resp, mmdb, false, true, query);
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "HTTP error 404 unknown path '" + path + "' desu~");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static void printMlistShort (DataWriter dw, MediaListReference listRef, Collection<Player> players) throws SAXException {
		String fileName = listRef.getIdentifier().substring(listRef.getIdentifier().lastIndexOf(File.separator) + 1);

		FeedHelper.addElement(dw, "title", listRef.getTitle());

		String type;
		switch (listRef.getType()) {
			case LOCALMMDB:
				type = ILocalMixedMediaDb.TYPE;
				break;
			case REMOTEMMDB:
				type = IRemoteMixedMediaDb.TYPE;
				break;
			default:
				throw new IllegalArgumentException("Can not list type '" + listRef.getType() + "' desu~");
		}
		FeedHelper.addLink(dw, CONTEXTPATH + "/" + type + "/" + fileName, "self", "text/xml");

		for (Player p : players) {
			FeedHelper.addLink(dw, "/player/" + p.getId() + "/play/" + fileName, "play", "cmd");
		}
	}

	private static void printMlistLong (HttpServletResponse resp, IMixedMediaDb ml, boolean listSrcs, boolean listItems) throws SAXException, MorriganException, DbException, IOException {
		printMlistLong(resp, ml, listSrcs, listItems, null);
	}

	private static void printMlistLong (HttpServletResponse resp, IMixedMediaDb ml, boolean listSrcs, boolean listItems, String queryString) throws SAXException, MorriganException, DbException, IOException {
		printMlistLong(resp, ml, listSrcs, listItems, true, queryString); // TODO always include tags?
	}

	private static void printMlistLong (HttpServletResponse resp, IMixedMediaDb ml, boolean listSrcs, boolean listItems, boolean includeTags, String queryString) throws SAXException, MorriganException, DbException, IOException {
		ml.read();
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "mlist");

		List<IMixedMediaItem> items;
		if (queryString != null) {
			items = ml.simpleSearch(queryString, MAX_RESULTS);
		}
		else {
			items = ml.getMediaItems();
		}

		String listFile;
		try {
			listFile = URLEncoder.encode(FeedHelper.filenameFromPath(ml.getListId()), "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		dw.dataElement("title", ml.getListName());
		if (queryString != null) dw.dataElement("query", queryString);
		dw.dataElement("count", String.valueOf(items.size()));

		// TODO calculate these values from query results.
		if (queryString == null) {
			DurationData totalDuration = ml.getTotalDuration();
			dw.dataElement("duration", String.valueOf(totalDuration.getDuration()));
			dw.dataElement("durationcomplete", String.valueOf(totalDuration.isComplete()));

			dw.dataElement("defaulttype", String.valueOf(ml.getDefaultMediaType().getN()));
			dw.dataElement("sortcolumn", ml.getSort().getHumanName());
			dw.dataElement("sortdirection", ml.getSortDirection().toString());
		}

		String pathToSelf = CONTEXTPATH + "/" + ml.getType() + "/" + listFile;
		FeedHelper.addLink(dw, pathToSelf, "self", "text/xml");
		if (!listItems) FeedHelper.addLink(dw, pathToSelf + "/" + PATH_ITEMS, PATH_ITEMS, "text/xml");
		FeedHelper.addLink(dw, pathToSelf + "/" + PATH_ALBUMS, PATH_ALBUMS, "text/xml");
		FeedHelper.addLink(dw, pathToSelf + "/" + PATH_SRC, PATH_SRC, "text/xml");

		if (listSrcs) {
			for (String s : ml.getSources()) {
				FeedHelper.addElement(dw, "src", s);
			}
		}

		if (listItems) {
			for (IMixedMediaItem mi : items) {
				dw.startElement("entry");

				FeedHelper.addElement(dw, "title", mi.getTitle());

				FeedHelper.addLink(dw, fileLink(mi), "self"); // Path is relative to this feed.

				if (mi.getDateAdded() != null) {
					FeedHelper.addElement(dw, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
				}
				if (mi.getDateLastModified() != null) {
					FeedHelper.addElement(dw, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
				}
				FeedHelper.addElement(dw, "type", mi.getMediaType().getN());
				if (mi.getHashcode() != null && !BigInteger.ZERO.equals(mi.getHashcode())) FeedHelper.addElement(dw, "hash", mi.getHashcode().toString(16));
				FeedHelper.addElement(dw, "enabled", Boolean.toString(mi.isEnabled()));
				FeedHelper.addElement(dw, "missing", Boolean.toString(mi.isMissing()));

				if (mi.getMediaType() == MediaType.TRACK) {
					FeedHelper.addElement(dw, "duration", mi.getDuration());
					FeedHelper.addElement(dw, "startcount", mi.getStartCount());
					FeedHelper.addElement(dw, "endcount", mi.getEndCount());
					if (mi.getDateLastPlayed() != null) {
						FeedHelper.addElement(dw, "datelastplayed", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastPlayed()));
					}
				}
				else if (mi.getMediaType() == MediaType.PICTURE) {
					FeedHelper.addElement(dw, "width", mi.getWidth());
					FeedHelper.addElement(dw, "height", mi.getHeight());
				}

				if (includeTags) {
					List<MediaTag> tags = ml.getTags(mi);
					for (MediaTag tag : tags) {
						FeedHelper.addElement(dw, "tag", tag.getTag(), new String[][] {
								{ "t", String.valueOf(tag.getType().getIndex()) },
								{ "c", tag.getClassification() == null ? "" : tag.getClassification().getClassification() }
						});
					}
				}

				dw.endElement("entry");
			}
		}

		FeedHelper.endDocument(dw, "mlist");
	}

	private static void printAlbums (HttpServletResponse resp, IMixedMediaDb ml) throws SAXException, IOException, MorriganException {
		ml.read();
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "albums");
		for (MediaAlbum album : ml.getAlbums()) {
			dw.startElement("entry");
			FeedHelper.addElement(dw, "name", album.getName());

			Collection<IMixedMediaItem> pics = ml.getAlbumItems(MediaType.PICTURE, album); // TODO set max result count.
			if (pics != null && pics.size() >= 1) {
				IMixedMediaItem pic = pics.iterator().next();
				FeedHelper.addLink(dw, fileLink(pic), "cover");
			}

			dw.endElement("entry");
		}
		FeedHelper.endDocument(dw, "albums");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static String fileLink (IMixedMediaItem mi) {
		try {
			return URLEncoder.encode(mi.getFilepath(), "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
