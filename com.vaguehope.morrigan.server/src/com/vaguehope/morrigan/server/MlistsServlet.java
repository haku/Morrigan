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
import com.vaguehope.morrigan.model.media.IAbstractMixedMediaDb;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.impl.MediaFactoryImpl;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.LocalMixedMediaDbHelper;
import com.vaguehope.morrigan.player.IPlayerLocal;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.server.feedwriters.AbstractFeed;
import com.vaguehope.morrigan.server.feedwriters.XmlHelper;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDb;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbHelper;
import com.vaguehope.sqlitewrapper.DbException;

/**
 * Valid URLs:
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
 * 
 *  GET /mlists/LOCALMMDB/wui.local.db3/query/example
 * </pre>
 */
public class MlistsServlet extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String CONTEXTPATH = "/mlists";
	
	public static final String PATH_ITEMS = "items";
	public static final String PATH_SRC = "src";
	public static final String PATH_QUERY = "query";
	
	public static final String CMD_NEWMMDB = "newmmdb";
	public static final String CMD_SCAN = "scan";
	public static final String CMD_PLAY = "play";
	public static final String CMD_QUEUE = "queue";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final long serialVersionUID = 2754601524882233866L;
	
	private static final String ROOTPATH = "/";
	private static final int MAX_RESULTS = 50;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			processRequest(Verb.GET, req, resp, null);
		}
		catch (DbException e) {
			throw new ServletException(e);
		} catch (SAXException e) {
			throw new ServletException(e);
		} catch (MorriganException e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
    		String act = req.getParameter("action");
    		if (act == null) {
    			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    			resp.setContentType("text/plain");
    			resp.getWriter().println("HTTP Error 400 'action' parameter not set desu~");
    		}
    		else {
    			processRequest(Verb.POST, req, resp, act);
    		}
		}
		catch (DbException e) {
			throw new ServletException(e);
		} catch (SAXException e) {
			throw new ServletException(e);
		} catch (MorriganException e) {
			throw new ServletException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private enum Verb {GET, POST}
	
	/**
	 * Param action will not be null when verb==POST.
	 */
	private static void processRequest (Verb verb, HttpServletRequest req, HttpServletResponse resp, String action) throws IOException, DbException, SAXException, MorriganException {
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
						IAbstractMixedMediaDb<?> mmdb;
						if (type.equals(ILocalMixedMediaDb.TYPE)) {
							String f = LocalMixedMediaDbHelper.getFullPathToMmdb(pathParts[1]);
							mmdb = MediaFactoryImpl.get().getLocalMixedMediaDb(f);
						}
						else if (type.equals(IRemoteMixedMediaDb.TYPE)) {
							String f = RemoteMixedMediaDbHelper.getFullPathToMmdb(pathParts[1]);
							mmdb = RemoteMixedMediaDb.FACTORY.manufacture(f);
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
						resp.setContentType("text/plain");
						resp.getWriter().println("Unknown type '"+type+"' desu~.");
					}
				}
				else {
					resp.setContentType("text/plain");
					resp.getWriter().println("Invalid request '"+path+"' desu~");
				}
			}
			else {
				resp.setContentType("text/plain");
				resp.getWriter().println("Invalid request '"+path+"' desu~");
			}
		}
	}
	
	private static void postToRoot(HttpServletResponse resp, String action) throws IOException {
		if (action.equals(CMD_NEWMMDB)) {
			resp.setContentType("text/plain");
			resp.getWriter().println("TODO implement create new MMDB cmd desu~");
		}
		else {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("text/plain");
			resp.getWriter().println("HTTP error 400 '"+action+"' is not a valid action parameter desu~");
		}
	}
	
	private static void postToMmdb(HttpServletRequest req, HttpServletResponse resp, String action, IAbstractMixedMediaDb<?> mmdb, String path, String afterPath) throws IOException, MorriganException, DbException {
		if (action.equals(CMD_PLAY) || action.equals(CMD_QUEUE)) {
			String playerIdS = req.getParameter("playerid");
			if (playerIdS == null) {
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				resp.setContentType("text/plain");
				resp.getWriter().println("HTTP error 400 'playerId' parameter not set desu~");
			}
			else {
				int playerId = Integer.parseInt(playerIdS);
				IPlayerLocal player = PlayerRegister.getLocalPlayer(playerId);
				
				mmdb.read(); // TODO make this call only when needed?  This is a bit catch-all.
				
				if (path != null && path.equals(PATH_ITEMS) && afterPath != null && afterPath.length() > 0) {
					String filepath = URLDecoder.decode(afterPath, "UTF-8");
					if (mmdb.hasFile(filepath)) {
						IMixedMediaItem item = mmdb.getByFile(filepath);
						if (item != null) {
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
						else {
							resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
							resp.setContentType("text/plain");
							resp.getWriter().println("Failed to retrieve file '"+filepath+"' from MMDB when it should have been there desu~.");
						}
					}
					else {
						resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
						resp.setContentType("text/plain");
						resp.getWriter().println("HTTP error 404 file '"+filepath+"' not found in MMDB '"+mmdb.getListName()+"' desu~");
					}
				}
				else {
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
		}
		else if (action.equals(CMD_SCAN)) {
			HeadlessHelper.scheduleMmdbScan(mmdb);
			resp.setContentType("text/plain");
			resp.getWriter().println("Scan scheduled desu~");
		}
		else {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.setContentType("text/plain");
			resp.getWriter().println("HTTP error 400 '"+action+"' is not a valid action parameter desu~");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void printMlistList (HttpServletResponse resp) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = AbstractFeed.startFeed(resp.getWriter());
		
		AbstractFeed.addElement(dw, "title", "Morrigan media lists desu~");
		AbstractFeed.addLink(dw, CONTEXTPATH, "self", "text/xml");
		
		Collection<IPlayerLocal> players = PlayerRegister.getLocalPlayers();
		
		// TODO merge 2 loops.
		
		for (MediaListReference listRef : MediaFactoryImpl.get().getAllLocalMixedMediaDbs()) {
			dw.startElement("entry");
			printMlistShort(dw, listRef, players);
			dw.endElement("entry");
		}
		
		for (MediaListReference listRef : RemoteMixedMediaDbHelper.getAllRemoteMmdb()) {
			dw.startElement("entry");
			printMlistShort(dw, listRef, players);
			dw.endElement("entry");
		}
		
		AbstractFeed.endFeed(dw);
	}
	
	static private void getToMmdb (HttpServletResponse resp, IAbstractMixedMediaDb<?> mmdb, String path, String afterPath) throws IOException, SAXException, MorriganException, DbException {
		if (path == null) {
			printMlistLong(resp, mmdb, false, false);
		}
		else if (path.equals(PATH_ITEMS)) {
			if (afterPath != null && afterPath.length() > 0) {
				// Request to fetch media file.
				String filepath = URLDecoder.decode(afterPath, "UTF-8");
				if (mmdb.hasFile(filepath)) {
					// First see if we have the file locally to send.
					File file = new File(filepath);
					if (file.exists()) {
						ServletHelper.returnFile(file, resp);
					}
					else {
						// Then see if the MMDB can find the file somewhere?
						IMixedMediaItem item = mmdb.getByFile(filepath);
						if (item != null) {
							ServletHelper.prepForReturnFile(item.getTitle(), 0, resp); // TODO pass through length?
							mmdb.copyItemFile(item, resp.getOutputStream());
							resp.flushBuffer();
						}
						else { // OK give up - no idea where this file is supposed to be.
							resp.reset();
							resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
							resp.setContentType("text/plain");
							resp.getWriter().println("HTTP error 404 '"+filepath+"' in list but not availabe desu~");
						}
					}
				}
				else {
					resp.reset();
					resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
					resp.setContentType("text/plain");
					resp.getWriter().println("HTTP error 404 '"+filepath+"' is not in '"+mmdb.getListId()+"' desu~");
				}
			}
			else {
				printMlistLong(resp, mmdb, false, true);
			}
		}
		else if (path.equals(PATH_SRC)) {
			printMlistLong(resp, mmdb, true, false);
		}
		else if (path.equals(PATH_QUERY) && afterPath != null && afterPath.length() > 0) {
			String query = URLDecoder.decode(afterPath, "UTF-8");
			printMlistLong(resp, mmdb, false, true, query);
		}
		else {
			resp.reset();
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			resp.setContentType("text/plain");
			resp.getWriter().println("HTTP error 404 unknown path '"+path+"' desu~");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void printMlistShort (DataWriter dw, MediaListReference listRef, Collection<IPlayerLocal> players) throws SAXException {
		String fileName = listRef.getIdentifier().substring(listRef.getIdentifier().lastIndexOf(File.separator) + 1);
		
		AbstractFeed.addElement(dw, "title", listRef.getTitle());
		
		String type;
		switch (listRef.getType()) {
			case LOCALMMDB:  type = ILocalMixedMediaDb.TYPE;  break;
			case REMOTEMMDB: type = IRemoteMixedMediaDb.TYPE; break;
			default: throw new IllegalArgumentException("Can not list type '"+listRef.getType()+"' desu~");
		}
		AbstractFeed.addLink(dw, CONTEXTPATH + "/" + type + "/" + fileName, "self", "text/xml");
		
		for (IPlayerLocal p : players) {
			AbstractFeed.addLink(dw, "/player/" + p.getId() + "/play/" + fileName, "play", "cmd");
		}
	}
	
	static private void printMlistLong (HttpServletResponse resp, IAbstractMixedMediaDb<?> ml, boolean listSrcs, boolean listItems) throws SAXException, MorriganException, DbException, IOException {
		printMlistLong(resp, ml, listSrcs, listItems, null);
	}
	
	static private void printMlistLong (HttpServletResponse resp, IAbstractMixedMediaDb<?> ml, boolean listSrcs, boolean listItems, String queryString) throws SAXException, MorriganException, DbException, IOException {
		printMlistLong(resp, ml, listSrcs, listItems, true, queryString); // TODO always include tags?
	}
	
	static private void printMlistLong (HttpServletResponse resp, IAbstractMixedMediaDb<?> ml, boolean listSrcs, boolean listItems, boolean includeTags, String queryString) throws SAXException, MorriganException, DbException, IOException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = AbstractFeed.startDocument(resp.getWriter(), "mlist");
		
		ml.read();
		
		List<IMixedMediaItem> items;
		if (queryString != null) {
			items = ml.simpleSearch(queryString, MAX_RESULTS);
		}
		else {
			items = ml.getMediaItems();
		}
		
		String listFile;
		try {
			listFile = URLEncoder.encode(AbstractFeed.filenameFromPath(ml.getListId()), "UTF-8");
		} catch (UnsupportedEncodingException e) {
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
		AbstractFeed.addLink(dw, pathToSelf, "self", "text/xml");
		if (!listItems) AbstractFeed.addLink(dw, pathToSelf + "/" + PATH_ITEMS, PATH_ITEMS, "text/xml");
		AbstractFeed.addLink(dw, pathToSelf + "/" + PATH_SRC, PATH_SRC, "text/xml");
		
		if (listSrcs) {
			List<String> src;
			src = ml.getSources();
			
			for (String s : src) {
				AbstractFeed.addElement(dw, "src", s);
			}
		}
		
		if (listItems) {
			for (IMixedMediaItem mi : items) {
    			dw.startElement("entry");
    			
    			AbstractFeed.addElement(dw, "title", mi.getTitle());
    			
    			String file;
    			try {
    				file = URLEncoder.encode(mi.getFilepath(), "UTF-8");
    			} catch (UnsupportedEncodingException e) {
    				throw new RuntimeException(e);
    			}
    			AbstractFeed.addLink(dw, file, "self"); // Path is relative to this feed.
    			
    			if (mi.getDateAdded() != null) {
    				AbstractFeed.addElement(dw, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
    			}
    			if (mi.getDateLastModified() != null) {
    				AbstractFeed.addElement(dw, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
    			}
    			AbstractFeed.addElement(dw, "type", mi.getMediaType().getN());
    			if (mi.getHashcode() != null && !BigInteger.ZERO.equals(mi.getHashcode())) AbstractFeed.addElement(dw, "hash", mi.getHashcode().toString(16));
    			AbstractFeed.addElement(dw, "enabled", Boolean.toString(mi.isEnabled()));
    			AbstractFeed.addElement(dw, "missing", Boolean.toString(mi.isMissing()));
    			
    			if (mi.getMediaType() == MediaType.TRACK) {
    				AbstractFeed.addElement(dw, "duration", mi.getDuration());
        			AbstractFeed.addElement(dw, "startcount", mi.getStartCount());
        			AbstractFeed.addElement(dw, "endcount", mi.getEndCount());
        			if (mi.getDateLastPlayed() != null) {
        				AbstractFeed.addElement(dw, "datelastplayed", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastPlayed()));
        			}
    			}
    			else if (mi.getMediaType() == MediaType.PICTURE) {
    				AbstractFeed.addElement(dw, "width", mi.getWidth());
    				AbstractFeed.addElement(dw, "height", mi.getHeight());
    			}
    			
    			if (includeTags) {
    				List<MediaTag> tags = ml.getTags(mi);
    				for (MediaTag tag : tags) {
						AbstractFeed.addElement(dw, "tag", tag.getTag(), new String[][] {
							{"t", String.valueOf(tag.getType().getIndex())},
							{"c", tag.getClassification() == null ? "" : tag.getClassification().getClassification()}
							});
					}
    			}
    			
    			dw.endElement("entry");
    		}
		}
		
		AbstractFeed.endDocument(dw, "mlist");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
