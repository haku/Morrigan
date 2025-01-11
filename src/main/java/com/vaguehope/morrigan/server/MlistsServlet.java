package com.vaguehope.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.ajax.JSON;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.megginson.sax.DataWriter;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.ListRefWithTitle;
import com.vaguehope.morrigan.model.media.MatchMode;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.SortColumn;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.server.util.FeedHelper;
import com.vaguehope.morrigan.server.util.ImageResizer;
import com.vaguehope.morrigan.server.util.XmlHelper;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.AsyncTask;
import com.vaguehope.morrigan.transcode.Transcode;
import com.vaguehope.morrigan.transcode.TranscodeContext;
import com.vaguehope.morrigan.transcode.TranscodeProfile;
import com.vaguehope.morrigan.transcode.Transcoder;
import com.vaguehope.morrigan.util.ChecksumCache;
import com.vaguehope.morrigan.util.MnLogger;
import com.vaguehope.morrigan.util.StringHelper;

/**
 * Valid URLs:
 *
 * <pre>
 *  GET /mlists
 *  GET /mlists/savedviews
 *
 *  GET /mlists/LOCALMMDB/example.local.db3
 *  GET /mlists/LOCALMMDB/example.local.db3/src
 * POST /mlists/LOCALMMDB/example.local.db3 action=play&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3 action=queue&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3 view=myview&action=play&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3 view=myview&action=queue&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3 action=scan
 * POST /mlists/LOCALMMDB/example.local.db3 action=pull&remote=someremote
 *
 *  GET /mlists/LOCALMMDB/example.local.db3/tags?term=foo
 *
 *  GET /mlists/LOCALMMDB/example.local.db3/items
 *  GET /mlists/LOCALMMDB/example.local.db3/items?includeddeletedtags=true
 *  GET /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3
 *  GET /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp4?transcode=some_transcode
 *  GET /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.jpg?resize=128
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3 action=play&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3 action=queue&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3 view=myview&action=play&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3 view=myview&action=queue&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3 action=addtag&tag=foo
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3 action=rmtag&tag=foo
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp3 action=set_enabled&enabled=true
 * POST /mlists/LOCALMMDB/example.local.db3/items/%2Fhome%2Fhaku%2Fmedia%2Fmusic%2Fsong.mp4?transcode=some_transcode action=transcode
 *
 *  GET /mlists/LOCALMMDB/example.local.db3/albums
 *  GET /mlists/LOCALMMDB/example.local.db3/albums/somealbum
 * POST /mlists/LOCALMMDB/example.local.db3/albums/somealbum action=play&playerid=0
 * POST /mlists/LOCALMMDB/example.local.db3/albums/somealbum action=queue&playerid=0
 *
 *  GET /mlists/LOCALMMDB/example.local.db3/query/example
 *  GET /mlists/LOCALMMDB/example.local.db3/query/example?maxresults=0         // No limit.
 *  GET /mlists/LOCALMMDB/example.local.db3/query/example?maxresults=500
 *  GET /mlists/LOCALMMDB/example.local.db3/query/example?includedisabled=true
 *  GET /mlists/LOCALMMDB/example.local.db3/query/example?column=foo&order=asc
 *  GET /mlists/LOCALMMDB/example.local.db3/query/example?transcode=some_transcode
 *
 *  GET /mlists/LOCALMMDB/example.local.db3/sha1tags
 *  GET /mlists/LOCALMMDB/example.local.db3/sha1tags?includeautotags=true
 * </pre>
 */
public class MlistsServlet extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String REL_CONTEXTPATH = "mlists";
	public static final String CONTEXTPATH = "/" + REL_CONTEXTPATH;

	private static final String PATH_SAVED_VIEWS = "/savedviews";

	private static final String PATH_SRC = "src";
	private static final String PATH_TAGS = "tags";
	public static final String PATH_ITEMS = "items";
	private static final String PATH_ALBUMS = "albums";
	private static final String PATH_QUERY = "query";
	private static final String PATH_SHA1TAGS = "sha1tags";

	static final String PARAM_TERM = "term";
	private static final String PARAM_COUNT = "count";
	public static final String PARAM_INCLUDE_DELETED_TAGS = "includeddeletedtags";
	private static final String PARAM_RESIZE = "resize";
	private static final String PARAM_ACTION = "action";
	private static final String PARAM_PLAYERID = "playerid";
	private static final String PARAM_TAG = "tag";
	private static final String PARAM_VIEW = "view";
	private static final String PARAM_COLUMN = "column";
	private static final String PARAM_ORDER = "order";
	private static final String PARAM_MAXRESULTS = "maxresults";
	private static final String PARAM_INCLUDE_DISABLED = "includedisabled";
	private static final String PARAM_TRANSCODE = "transcode";
	private static final String PARAM_ENABLED = "enabled";
	private static final String PARAM_INCLUDE_AUTO_TAGS = "includeautotags";

	public static final String CMD_NEWMMDB = "newmmdb";
	public static final String CMD_SCAN = "scan";
	public static final String CMD_PLAY = "play";
	public static final String CMD_QUEUE = "queue";
	public static final String CMD_QUEUE_TOP = "queue_top";
	public static final String CMD_ADDTAG = "addtag";
	public static final String CMD_RMTAG = "rmtag";
	public static final String CMD_SET_ENABLED = "set_enabled";
	public static final String CMD_TRANSCODE = "transcode";

	private static final String CONTENT_TYPE_JSON = "text/json;charset=utf-8";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final long serialVersionUID = 2754601524882233866L;

	private static final String ROOTPATH = "/";
	private static final int DEFAULT_MAX_QUERY_RESULTS = 250;

	private static final MnLogger LOG = MnLogger.make(MlistsServlet.class);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final PlayerReader playerListener;
	private final MediaFactory mediaFactory;
	private final AsyncActions asyncActions;
	private final Transcoder transcoder;
	private final Config config;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MlistsServlet (final PlayerReader playerListener, final MediaFactory mediaFactory,
			final AsyncActions asyncActions, final Transcoder transcoder, final Config config) {
		this.playerListener = playerListener;
		this.mediaFactory = mediaFactory;
		this.asyncActions = asyncActions;
		this.transcoder = transcoder;
		this.config = config;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			processRequest(Verb.GET, req, resp, null);
		}
		catch (final Exception e) {
			logAndWrap(e, resp);
		}
	}

	@Override
	protected void doPost (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			final String act = req.getParameter(PARAM_ACTION);
			if (act == null) {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP Error 400 'action' parameter not set desu~");
			}
			else {
				processRequest(Verb.POST, req, resp, act);
			}
		}
		catch (final Exception e) {
			logAndWrap(e, resp);
		}
	}

	private static void logAndWrap (final Exception e, final HttpServletResponse resp) throws ServletException {
		if (resp.isCommitted()) {
			LOG.e("Error while writing committed response.", e);
		}
		throw new ServletException(e);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static enum Verb {
		GET, POST
	}

	/**
	 * Param action will not be null when verb==POST.
	 */
	private void processRequest (final Verb verb, final HttpServletRequest req, final HttpServletResponse resp, final String action) throws IOException, DbException, SAXException, MorriganException {
		final String reqPath = ServletHelper.getReqPath(req, REL_CONTEXTPATH);
		if (reqPath == null || reqPath.length() < 1 || reqPath.equals(ROOTPATH)) {
			if (verb == Verb.POST) {
				postToRoot(resp, action);
			}
			else {
				printMlistList(resp);
			}
		}
		else if (PATH_SAVED_VIEWS.equals(reqPath)) {
			printSavedViews(resp);
		}
		else {
			final String path = reqPath.startsWith(ROOTPATH) ? reqPath.substring(ROOTPATH.length()) : reqPath;
			if (path.length() > 0) {
				final String[] pathParts = path.split("/");
				if (pathParts.length >= 1) {
					final String filter = StringHelper.trimToNull(req.getParameter(PARAM_VIEW));
					ListRef ref = ListRef.fromUrlForm(pathParts[0]);
					if (filter != null) ref = ref.withSearch(filter);
					final MediaList mmdb = this.mediaFactory.getList(ref);
					mmdb.read();
					final String subPath = pathParts.length >= 2 ? pathParts[1] : null;
					final String afterSubPath = pathParts.length >= 3 ? pathParts[2] : null;
					if (verb == Verb.POST) {
						postToMmdb(req, resp, action, mmdb, subPath, afterSubPath);
					}
					else {
						getToMmdb(req, resp, mmdb, subPath, afterSubPath);
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

	private static void postToRoot (final HttpServletResponse resp, final String action) throws IOException {
		if (action.equals(CMD_NEWMMDB)) {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "TODO implement create new MMDB cmd desu~");
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP error 400 '" + action + "' is not a valid action parameter desu~");
		}
	}

	private void postToMmdb (final HttpServletRequest req, final HttpServletResponse resp, final String action, final MediaList mmdb, final String path, final String afterPath) throws IOException, MorriganException, DbException {
		if (path != null && path.equals(PATH_ITEMS) && afterPath != null && afterPath.length() > 0) {
			final String filepath = URLDecoder.decode(afterPath, "UTF-8");
			if (mmdb.hasFile(filepath).isKnown()) {
				final MediaItem item = mmdb.getByFile(filepath);
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
		else if (path != null && path.equals(PATH_ALBUMS) && afterPath != null && afterPath.length() > 0) {
			final String albumName = URLDecoder.decode(afterPath, "UTF-8");
			final MediaAlbum album = mmdb.getAlbum(albumName);
			if (album != null) {
				postToMmdbAlbum(req, resp, action, mmdb, album);
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "HTTP error 404 unknown album '" + albumName + "' desu~");
			}
		}
		else {
			postToMmdb(req, resp, action, mmdb);
		}
	}

	@SuppressWarnings("resource")
	private void postToMmdb (final HttpServletRequest req, final HttpServletResponse resp, final String action, final MediaList mmdb) throws IOException, MorriganException, DbException {
		if (action.equals(CMD_PLAY) || action.equals(CMD_QUEUE) || action.equals(CMD_QUEUE_TOP)) {
			final Player player = parsePlayer(req, resp);
			if (player != null) { // parsePlayer() will write the error msg.
				resp.setContentType("text/plain");
				mmdb.read();
				if (action.equals(CMD_PLAY)) {
					player.loadAndStartPlaying(mmdb);
					resp.getWriter().println("MMDB playing desu~");
				}
				else if (action.equals(CMD_QUEUE)) {
					player.getQueue().addToQueue(new PlayItem(mmdb, null));
					resp.getWriter().println("MMDB added to queue desu~");
				}
				else if (action.equals(CMD_QUEUE_TOP)) {
					player.getQueue().addToQueueTop(new PlayItem(mmdb, null));
					resp.getWriter().println("MMDB added to queue top desu~");
				}
				else {
					throw new IllegalArgumentException("The world has exploded desu~.");
				}
			}
		}
		else if (action.equals(CMD_SCAN)) {
			if (mmdb instanceof MediaDb) {
				final AsyncTask at = this.asyncActions.scheduleMmdbScan((MediaDb) mmdb);
				resp.setContentType("text/plain");
				resp.getWriter().println("Scan scheduled desu~");
				resp.getWriter().println("id=" + at.id());
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Only supported on DBs: " + action);
			}
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP error 400 '" + action + "' is not a valid action parameter desu~");
		}
	}

	@SuppressWarnings("resource")
	private void postToMmdbItem (final HttpServletRequest req, final HttpServletResponse resp, final String action, final MediaList mmdb, final MediaItem item) throws IOException, MorriganException {
		if (action.equals(CMD_PLAY) || action.equals(CMD_QUEUE) || action.equals(CMD_QUEUE_TOP)) {
			final Player player = parsePlayer(req, resp);
			if (player != null) { // parsePlayer() will write the error msg.
				resp.setContentType("text/plain");
				mmdb.read();
				if (action.equals(CMD_PLAY)) {
					player.loadAndStartPlaying(mmdb, item);
					resp.getWriter().println("Item playing desu~");
				}
				else if (action.equals(CMD_QUEUE)) {
					player.getQueue().addToQueue(new PlayItem(mmdb, item));
					resp.getWriter().println("Item added to queue desu~");
				}
				else if (action.equals(CMD_QUEUE_TOP)) {
					player.getQueue().addToQueueTop(new PlayItem(mmdb, item));
					resp.getWriter().println("Item added to queue top desu~");
				}
				else {
					throw new IllegalArgumentException("The world has exploded desu~.");
				}
			}
		}
		else if (action.equals(CMD_ADDTAG)) {
			final String tag = StringHelper.trimToNull(req.getParameter(PARAM_TAG));
			if (tag != null && tag.length() > 0) {
				mmdb.addTag(item, tag, MediaTagType.MANUAL, (MediaTagClassification) null);
				resp.setContentType("text/plain");
				resp.getWriter().println("Tag '" + tag + "' added desu~");
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'tag' parameter not set desu~");
			}
		}
		else if (action.equals(CMD_RMTAG)) {
			final String tag = req.getParameter(PARAM_TAG);
			if (tag != null && tag.length() > 0) {
				final List<MediaTag> tags = mmdb.getTags(item);
				boolean found = false;
				for (final MediaTag mt : tags) {
					if (mt.getType() == MediaTagType.MANUAL && mt.getClassification() == null && tag.equals(mt.getTag())) {
						mmdb.removeTag(mt);
						found = true;
						break;
					}
				}
				if (found) {
					resp.setContentType("text/plain");
					resp.getWriter().println("Tag '" + tag + "' removed desu~");
				}
				else {
					ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "tag '" + tag + "' not found desu~");
				}
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "'tag' parameter not set desu~");
			}
		}
		else if (action.equals(CMD_SET_ENABLED)) {
			final Boolean enab = ServletHelper.readParamBoolean(req, PARAM_ENABLED);
			if (enab != null) {
				mmdb.setItemEnabled(item, enab);
				resp.setContentType("text/plain");
				resp.getWriter().println("enabled=" + enab + " set desu~");
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing " + PARAM_ENABLED + " param.");
			}
		}
		else if (action.equals(CMD_TRANSCODE)) {
			final String transcode = StringHelper.trimToNull(req.getParameter(PARAM_TRANSCODE));
			if (transcode != null) {
				final TranscodeProfile tProfile = Transcode.parse(transcode).profileForItem(new TranscodeContext(this.config), mmdb, item);
				this.transcoder.transcodeToFile(tProfile);
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid or missing " + PARAM_TRANSCODE + " param.");
			}
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP error 400 '" + action + "' is not a valid action parameter desu~");
		}
	}

	@SuppressWarnings("resource")
	private void postToMmdbAlbum (final HttpServletRequest req, final HttpServletResponse resp, final String action, final MediaList mmdb, final MediaAlbum album) throws IOException, MorriganException {
		if (action.equals(CMD_PLAY) || action.equals(CMD_QUEUE) || action.equals(CMD_QUEUE_TOP)) {
			final Player player = parsePlayer(req, resp);
			if (player != null) { // parsePlayer() will write the error msg.
				mmdb.read();
				resp.setContentType("text/plain");
				final Collection<MediaItem> tracks = mmdb.getAlbumItems(MediaType.TRACK, album);
				final List<PlayItem> trackPlayItems = new ArrayList<>();
				for (final MediaItem track : tracks) {
					trackPlayItems.add(new PlayItem(mmdb, track));
				}
				if (action.equals(CMD_PLAY)) {
					player.getQueue().addToQueue(trackPlayItems);
					player.getQueue().moveInQueueEnd(trackPlayItems, false);
					player.nextTrack();
					resp.getWriter().println("Album playing desu~");
				}
				else if (action.equals(CMD_QUEUE)) {
					player.getQueue().addToQueue(trackPlayItems);
					resp.getWriter().println("Album added to queue desu~");
				}
				else if (action.equals(CMD_QUEUE_TOP)) {
					player.getQueue().addToQueueTop(trackPlayItems);
					resp.getWriter().println("Album added to queue top desu~");
				}
				else {
					throw new IllegalArgumentException("The world has exploded desu~.");
				}
			}
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP error 400 '" + action + "' is not a valid action parameter desu~");
		}
	}

	private Player parsePlayer (final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		final String playerId = req.getParameter(PARAM_PLAYERID);
		if (playerId == null) {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP error 400 'playerId' parameter not set desu~");
			return null;
		}
		return this.playerListener.getPlayer(playerId);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@SuppressWarnings("resource")
	private void printMlistList (final HttpServletResponse resp) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		final DataWriter dw = FeedHelper.startFeed(resp.getWriter());

		FeedHelper.addElement(dw, "title", "Morrigan media lists desu~");
		FeedHelper.addLink(dw, REL_CONTEXTPATH, "self", "text/xml");

		for (final ListRefWithTitle listRef : this.mediaFactory.allLists()) {
			FeedHelper.startElement(dw, "entry", new String[][] { { "type", listRef.getListRef().getType().toString() } });
			printMlistShort(dw, listRef);
			dw.endElement("entry");
		}

		FeedHelper.endFeed(dw);
	}

	@SuppressWarnings("resource")
	private void printSavedViews (final HttpServletResponse resp) throws IOException {
		final File file = this.config.getSavedViewsFile();
		if (!file.exists()) {
			resp.setContentType(CONTENT_TYPE_JSON);
			resp.getWriter().write("[]");
			return;
		}
		ServletHelper.returnFile(file, CONTENT_TYPE_JSON, null, null, resp);
	}

	@SuppressWarnings("resource")
	private void getToMmdb (final HttpServletRequest req, final HttpServletResponse resp, final MediaList mmdb, final String path, final String afterPath) throws IOException, SAXException, MorriganException, DbException {
		if (path == null) {
			printMlistLong(resp, mmdb, IncludeSrcs.NO, IncludeItems.NO, IncludeTags.NO);
		}
		else if (path.equals(PATH_ITEMS)) {
			if (afterPath != null && afterPath.length() > 0) {
				// Request to fetch media file.
				final String filepath = URLDecoder.decode(afterPath, "UTF-8");
				if (mmdb.hasFile(filepath).isKnown()) {
					final MediaItem item = mmdb.getByFile(filepath);
					final File file = item.getFile();
					if (file != null && file.exists()) {
						if (ServletHelper.checkCanReturn304(file.lastModified(), req, resp)) return;

						final String transcode = StringHelper.trimToNull(req.getParameter(PARAM_TRANSCODE));
						if (transcode != null) {
							final TranscodeProfile tProfile = Transcode.parse(transcode).profileForItem(new TranscodeContext(this.config), mmdb, item);
							final File transcodedFile = tProfile.getCacheFileIfFresh();
							if (transcodedFile != null) {
								ServletHelper.returnFile(transcodedFile, tProfile.getMimeType().getMimeType(), null, req.getHeader("Range"), resp);
							}
							else {
								ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "HTTP error 400 '" + filepath + "' has not been transcoded desu~");
							}
							return;
						}

						final Integer resize = ServletHelper.readParamInteger(req, PARAM_RESIZE);
						if (resize != null) {
							final File resizedFile = ImageResizer.resizeFile(file, resize, this.config);
							ServletHelper.returnFile(resizedFile, ImageResizer.FORMAT_TYPE.getMimeType(), null, req.getHeader("Range"), resp);
							return;
						}

						ServletHelper.returnFile(file, item.getMimeType(), null, req.getHeader("Range"), resp);
					}
					else {
						final long lastModified = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1); // Falsify.
						if (ServletHelper.checkCanReturn304(lastModified, req, resp)) return;
						// TODO pass through length and modified date?
						ServletHelper.prepForReturnFile(0, System.currentTimeMillis(), item.getMimeType(), null, resp);
						mmdb.copyItemFile(item, resp.getOutputStream());
						resp.flushBuffer();
					}
				}
				else {
					ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "HTTP error 404 '" + filepath + "' is not in '" + mmdb.getListRef() + "' desu~");
				}
			}
			else {
				final boolean includeDeletedTags = ServletHelper.readParamBoolean(req, PARAM_INCLUDE_DELETED_TAGS, false);
				printMlistLong(resp, mmdb, IncludeSrcs.NO, IncludeItems.YES,
						includeDeletedTags ? IncludeTags.YES_INCLUDING_DELETED : IncludeTags.YES);
			}
		}
		else if (path.equals(PATH_SRC)) {
			printMlistLong(resp, mmdb, IncludeSrcs.YES, IncludeItems.NO, IncludeTags.NO);
		}
		else if (path.equals(PATH_TAGS)) {
			final String term = StringHelper.trimToEmpty(req.getParameter(PARAM_TERM));
			Integer count = ServletHelper.readParamInteger(req, PARAM_COUNT);
			if (count == null || count < 1) count = 10;
			final Map<String, MediaTag> tags = mmdb.tagSearch(term, MatchMode.PREFIX, count);
			@SuppressWarnings("rawtypes")
			final Map[] arr = new Map[tags.size()];
			int i = 0;
			for (final Entry<String, MediaTag> tag : tags.entrySet()) {
				final Map<String, String> m = new HashMap<>(2);
				m.put("label", tag.getKey());
				m.put("value", tag.getValue().getTag());
				arr[i++] = m;
			}
			resp.setContentType("application/json");
			resp.getWriter().println(JSON.toString(arr));
		}
		else if (path.equals(PATH_ALBUMS)) {
			if (afterPath != null && afterPath.length() > 0) {
				final String albumName = URLDecoder.decode(afterPath, "UTF-8");
				final MediaAlbum album = mmdb.getAlbum(albumName);
				if (album != null) {
					printAlbum(resp, mmdb, album);
				}
				else {
					ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "HTTP error 404 unknown album '" + albumName + "' desu~");
				}
			}
			else {
				printAlbums(resp, mmdb);
			}
		}
		else if (path.equals(PATH_QUERY) && afterPath != null && afterPath.length() > 0) {
			final String query = URLDecoder.decode(afterPath, "UTF-8");
			final SortColumn[] sortColumns = parseSortColumns(req);
			final SortDirection[] sortDirections = parseSortOrder(req);
			final int maxResults = ServletHelper.readParamInteger(req, PARAM_MAXRESULTS, DEFAULT_MAX_QUERY_RESULTS);
			final boolean includeDisabled = ServletHelper.readParamBoolean(req, PARAM_INCLUDE_DISABLED, false);
			final String transcode = StringHelper.trimToNull(req.getParameter(PARAM_TRANSCODE));
			printMlistLong(resp, mmdb, IncludeSrcs.NO, IncludeItems.YES, IncludeTags.YES,
					query, maxResults, sortColumns, sortDirections, includeDisabled, transcode);
		}
		else if (path.equals(PATH_SHA1TAGS)) {
			if (mmdb instanceof MediaDb) {
				final MediaDb db = (MediaDb) mmdb;
				final boolean includeAutoTags = ServletHelper.readParamBoolean(req, PARAM_INCLUDE_AUTO_TAGS, false);
				// TODO it would be nice to make the gson instance static and reusable, but ATM the type converted wraps the DB.
				// TODO do not output entries that do not have any tags.
				final Gson gson = new GsonBuilder()
						.registerTypeHierarchyAdapter(MediaItem.class, new Sha1TagsJsonSerializer(db, includeAutoTags))
						.create();
				final Object[] items = db.getAllDbEntries().stream().filter(i -> i.getSha1() != null).toArray();
				resp.setContentType(CONTENT_TYPE_JSON);
				gson.toJson(items, resp.getWriter());
			}
			else {
				ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Only supported on local DBs: " + path);
			}
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "HTTP error 404 unknown path '" + path + "' desu~");
		}
	}

	private static class Sha1TagsJsonSerializer implements JsonSerializer<MediaItem> {
		private final MediaList db;
		private final boolean includeAutoTags;

		public Sha1TagsJsonSerializer(MediaList db, boolean includeAutoTags) {
			this.db = db;
			this.includeAutoTags = includeAutoTags;
		}

		@Override
		public JsonElement serialize(final MediaItem i, final Type typeOfSrc, final JsonSerializationContext context) {
			final JsonObject o = new JsonObject();
			o.addProperty("sha1", i.getSha1().toString(16));
			final JsonArray a = new JsonArray();
			try {
				for (final MediaTag tag : this.db.getTagsIncludingDeleted(i)) {
					if (this.includeAutoTags == false && tag.getType() != MediaTagType.MANUAL) continue;
					final JsonObject t = new JsonObject();
					t.addProperty("tag", tag.getTag());
					if (tag.getClassification() != null) t.addProperty("cls", tag.getClassification().getClassification());
					if (tag.getModified() != null) t.addProperty("mod", tag.getModified().getTime());
					t.addProperty("del", tag.isDeleted());
					a.add(t);
				}
			}
			catch (final MorriganException e) {
				throw new IllegalStateException(e);
			}
			o.add("tags", a);
			return o;
		}
	}

	private static SortColumn[] parseSortColumns (final HttpServletRequest req) {
		final String raw = StringHelper.downcase(StringHelper.trimToNull(req.getParameter(PARAM_COLUMN)));
		for (final SortColumn sortColumn : SortColumn.values()) {
			if (sortColumn.name().equalsIgnoreCase(raw)) {
				return new SortColumn[] { sortColumn }; // TODO append additional.
			}
		}
		return null;
	}

	private static SortDirection[] parseSortOrder (final HttpServletRequest req) {
		final String raw = StringHelper.downcase(StringHelper.trimToNull(req.getParameter(PARAM_ORDER)));
		if ("asc".equals(raw)) {
			return new SortDirection[] { SortDirection.ASC };
		}
		else if ("desc".equals(raw)) {
			return new SortDirection[] { SortDirection.DESC };
		}
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static void printMlistShort (final DataWriter dw, final ListRefWithTitle listRef) throws SAXException {
		FeedHelper.addElement(dw, "title", listRef.getTitle());
		FeedHelper.addLink(dw, REL_CONTEXTPATH + "/" + listRef.getListRef().toUrlForm(), "self", "text/xml");
	}

	private enum IncludeSrcs {
		NO, YES;
	}

	private enum IncludeItems {
		NO, YES;
	}

	enum IncludeTags {
		NO, YES, YES_INCLUDING_DELETED;
	}

	private void printMlistLong (final HttpServletResponse resp, final MediaList ml,
			final IncludeSrcs includeSrcs, final IncludeItems includeItems, final IncludeTags includeTags)
					throws SAXException, MorriganException, DbException, IOException {
		printMlistLong(resp, ml, includeSrcs, includeItems, includeTags, null, 0, null, null, false, null);
	}

	@SuppressWarnings("resource")
	private void printMlistLong (final HttpServletResponse resp, final MediaList ml,
			final IncludeSrcs includeSrcs, final IncludeItems includeItems, final IncludeTags includeTags,
			final String queryString, final int maxQueryResults,
			final SortColumn[] sortColumns, final SortDirection[] sortDirections, final boolean includeDisabled,
			final String transcode)
					throws SAXException, MorriganException, DbException, IOException {
		ml.read();
		resp.setContentType("text/xml;charset=utf-8");
		final DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "mlist");

		List<MediaItem> items;
		if (queryString != null) {
			items = ml.search(MediaType.TRACK, queryString, maxQueryResults, sortColumns, sortDirections, includeDisabled);
		}
		else {
			items = ml.getMediaItems();
		}

		dw.dataElement("title", ml.getListName());
		dw.dataElement("uuid", ml.getUuid().toString());
		if (queryString != null) dw.dataElement("query", queryString);
		dw.dataElement("count", String.valueOf(items.size()));

		// TODO calculate these values from query results.
		if (queryString == null) {
			final DurationData totalDuration = ml.getTotalDuration();
			dw.dataElement("duration", String.valueOf(totalDuration.getDuration()));
			dw.dataElement("durationcomplete", String.valueOf(totalDuration.isComplete()));

			if (ml instanceof MediaDb) {
				final MediaDb db = (MediaDb) ml;
				dw.dataElement("sortcolumn", db.getSortColumn().getUiName());
				dw.dataElement("sortdirection", db.getSortDirection().toString());
			}
		}

		final String pathToSelf = REL_CONTEXTPATH + "/" + ml.getListRef().toUrlForm();
		FeedHelper.addLink(dw, pathToSelf, "self", "text/xml");
		if (includeItems == IncludeItems.NO) FeedHelper.addLink(dw, pathToSelf + "/" + PATH_ITEMS, PATH_ITEMS, "text/xml");
		FeedHelper.addLink(dw, pathToSelf + "/" + PATH_ALBUMS, PATH_ALBUMS, "text/xml");
		FeedHelper.addLink(dw, pathToSelf + "/" + PATH_SRC, PATH_SRC, "text/xml");

		if (ml instanceof MediaDb) {
			final MediaDb db = (MediaDb) ml;
			for (final String remote : db.getRemotes().keySet()) {
				FeedHelper.addElement(dw, "remote", remote);
			}

			if (includeSrcs == IncludeSrcs.YES) {
				for (final String s : db.getSources()) {
					FeedHelper.addElement(dw, "src", s);
				}
			}
		}

		if (includeItems == IncludeItems.YES) {
			for (final MediaItem mi : items) {
				dw.startElement("entry");
				fillInMediaItem(dw, ml, mi, includeTags, transcode, this.config);
				dw.endElement("entry");
			}
		}

		FeedHelper.endDocument(dw, "mlist");
	}

	static void fillInMediaItem (final DataWriter dw, final MediaList ml, final MediaItem mi,
			final IncludeTags includeTags, final String transcodeStr, final Config config) throws SAXException, MorriganException, IOException {
		String title = mi.getTitle();
		long fileSize = mi.getFileSize();
		final BigInteger originalFileMd5 = mi.getMd5();
		BigInteger fileMd5 = originalFileMd5;
		String fileLink = fileLink(mi);

		ItemTags tags = null;  // Could be replaced with a nicer Memoise thingy?

		final Transcode transcode = Transcode.parse(transcodeStr);
		if (transcode != Transcode.NONE) {
			if (mi instanceof MediaItem) {
				if (tags == null) tags = ml.readTags(mi);
				final TranscodeProfile tProfile = transcode.profileForItem(new TranscodeContext(config), mi, tags);
				if (tProfile != null) {
					title = tProfile.getTranscodedTitle();

					final File transcodedFile = tProfile.getCacheFileIfFresh();
					fileSize = transcodedFile != null ? transcodedFile.length() : 0L;
					fileMd5 = transcodedFile != null ? ChecksumCache.readMd5(transcodedFile) : null;

					fileLink += "?" + PARAM_TRANSCODE + "=" + transcode.getSymbolicName();
				}
			}
			else {
				throw new IllegalArgumentException("Can not transcode non IMediaTrack: " + mi);
			}
		}

		FeedHelper.addElement(dw, "title", title);
		if (fileSize > 0) FeedHelper.addElement(dw, "filesize", fileSize);
		FeedHelper.addLink(dw, fileLink, "self"); // Path is relative to this feed.

		if (mi.getDateAdded() != null) {
			FeedHelper.addElement(dw, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
		}
		if (mi.getDateLastModified() != null) {
			FeedHelper.addElement(dw, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
		}

		if (mi instanceof MediaItem) {
			FeedHelper.addElement(dw, "type", mi.getMediaType().getN());
		}
		if (mi.getMimeType() != null) FeedHelper.addElement(dw, "mimetype", mi.getMimeType());
		if (originalFileMd5 != null && !BigInteger.ZERO.equals(originalFileMd5)) FeedHelper.addElement(dw, "originalhash", originalFileMd5.toString(16));
		if (fileMd5 != null && !BigInteger.ZERO.equals(fileMd5)) FeedHelper.addElement(dw, "hash", fileMd5.toString(16));
		if (mi.getSha1() != null && !BigInteger.ZERO.equals(mi.getSha1())) FeedHelper.addElement(dw, "sha1", mi.getSha1().toString(16));
		FeedHelper.addElement(dw, "enabled", Boolean.toString(mi.isEnabled()), new String[][] {
			{ "m", mi.enabledLastModified() == null || mi.enabledLastModified().getTime() < 1L ? "" : String.valueOf(mi.enabledLastModified().getTime()) },
		});
		FeedHelper.addElement(dw, "missing", Boolean.toString(mi.isMissing()));

		if (mi instanceof MediaItem) {
			final MediaItem track = mi;
			FeedHelper.addElement(dw, "duration", track.getDuration());
			FeedHelper.addElement(dw, "startcount", track.getStartCount());
			FeedHelper.addElement(dw, "endcount", track.getEndCount());
			if (track.getDateLastPlayed() != null) {
				FeedHelper.addElement(dw, "datelastplayed", XmlHelper.getIso8601UtcDateFormatter().format(track.getDateLastPlayed()));
			}
		}
		else if (mi instanceof MediaItem) {
			final MediaItem pic = mi;
			FeedHelper.addElement(dw, "width", pic.getWidth());
			FeedHelper.addElement(dw, "height", pic.getHeight());
		}

		if (includeTags == IncludeTags.YES || includeTags == IncludeTags.YES_INCLUDING_DELETED) {
			if (tags == null) tags = ml.readTags(mi);
			if (includeTags == IncludeTags.YES_INCLUDING_DELETED) {
				for (final MediaTag tag : tags.tagsIncludingDeleted()) {
					FeedHelper.addElement(dw, "tag", tag.getTag(), new String[][] {
						{ "t", String.valueOf(tag.getType().getIndex()) },
						{ "c", tag.getClassification() == null ? "" : tag.getClassification().getClassification() },
						{ "m", tag.getModified() == null || tag.getModified().getTime() < 1L ? "" : String.valueOf(tag.getModified().getTime()) },
						{ "d", String.valueOf(tag.isDeleted()) }
					});
				}
			}
			else {
				for (final MediaTag tag : tags.tagsIncludingDeleted()) {
					if (!tag.isDeleted()) {
						FeedHelper.addElement(dw, "tag", tag.getTag(), new String[][] {
							{ "t", String.valueOf(tag.getType().getIndex()) },
							{ "c", tag.getClassification() == null ? "" : tag.getClassification().getClassification() }
						});
					}
				}
			}
		}
	}

	@SuppressWarnings("resource")
	private static void printAlbums (final HttpServletResponse resp, final MediaList ml) throws SAXException, IOException, MorriganException {
		ml.read();
		resp.setContentType("text/xml;charset=utf-8");
		final DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "albums");
		for (final MediaAlbum album : ml.getAlbums()) {
			dw.startElement("entry");
			printAlbumBody(dw, ml, album);
			dw.endElement("entry");
		}
		FeedHelper.endDocument(dw, "albums");
	}

	@SuppressWarnings("resource")
	private static void printAlbum (final HttpServletResponse resp, final MediaList ml, final MediaAlbum album) throws SAXException, IOException, MorriganException {
		ml.read();
		resp.setContentType("text/xml;charset=utf-8");
		final DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "album");
		printAlbumBody(dw, ml, album);
		FeedHelper.endDocument(dw, "album");
	}

	public static void printAlbumBody (final DataWriter dw, final MediaList ml, final MediaAlbum album) throws SAXException, MorriganException {
		FeedHelper.addElement(dw, "name", album.getName());
		FeedHelper.addLink(dw, fileLink(album), "self");
		FeedHelper.addElement(dw, "trackcount", album.getTrackCount());
		final File artFile = ml.findAlbumCoverArt(album);
		if (artFile != null) {
			FeedHelper.addLink(dw, fileLink(artFile), "cover");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static String fileLink (final File f) {
		try {
			return URLEncoder.encode(f.getAbsolutePath(), "UTF-8");
		}
		catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String fileLink (final MediaItem mi) {
		try {
			if (StringHelper.notBlank(mi.getRemoteId())) return URLEncoder.encode(mi.getRemoteId(), "UTF-8");
			if (StringHelper.notBlank(mi.getFilepath())) return URLEncoder.encode(mi.getFilepath(), "UTF-8");
			return "";
		}
		catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static String fileLink (final MediaAlbum album) {
		try {
			return URLEncoder.encode(album.getName(), "UTF-8");
		}
		catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
