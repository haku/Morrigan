package net.sparktank.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.DurationData;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.media.MediaListReference;
import net.sparktank.morrigan.model.media.impl.MediaFactoryImpl;
import net.sparktank.morrigan.model.media.internal.LocalMixedMediaDbHelper;
import net.sparktank.morrigan.player.IPlayerLocal;
import net.sparktank.morrigan.player.PlayItem;
import net.sparktank.morrigan.player.PlayerRegister;
import net.sparktank.morrigan.server.feedwriters.AbstractFeed;
import net.sparktank.morrigan.server.feedwriters.XmlHelper;
import net.sparktank.sqlitewrapper.DbException;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;

public class MlistsServlet extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String CONTEXTPATH = "/mlists";
	
	public static final String PATH_ITEMS = "items";
	public static final String PATH_SRC = "src";
	public static final String PATH_ITEM = "item";
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
	
	static private enum Verb {GET, POST};
	
	/**
	 * Param action will not be null when verb==POST.
	 */
	private void processRequest (Verb verb, HttpServletRequest req, HttpServletResponse resp, String action) throws IOException, DbException, SAXException, MorriganException {
		String actualTarget = req.getRequestURI().substring(req.getContextPath().length());
		
		if (actualTarget.equals(ROOTPATH)) {
			if (verb == Verb.POST) {
				postToRoot(resp, action);
			}
			else {
				printMlistList(resp);
			}
		}
		else {
			String path = actualTarget.substring(ROOTPATH.length());
			if (path.length() > 0) {
				String[] pathParts = path.split("/");
				if (pathParts.length >= 2) {
					String type = pathParts[0];
					if (type.equals(ILocalMixedMediaDb.TYPE)) {
						String f = LocalMixedMediaDbHelper.getFullPathToMmdb(pathParts[1]);
						ILocalMixedMediaDb mmdb = MediaFactoryImpl.get().getLocalMixedMediaDb(f);
						String subPath = pathParts.length >= 3 ? pathParts[2] : null;
						String afterSubPath = pathParts.length >= 4 ? pathParts[3] : null;
						if (verb == Verb.POST) {
							postToMmdb(req, resp, action, mmdb, subPath, afterSubPath);
						}
						else {
							printMlist(resp, mmdb, subPath, afterSubPath);
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
	
	private void postToRoot(HttpServletResponse resp, String action) throws IOException {
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
	
	private void postToMmdb(HttpServletRequest req, HttpServletResponse resp, String action, ILocalMixedMediaDb mmdb, String path, String afterPath) throws IOException, MorriganException, DbException {
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
				
				if (path.equals(PATH_ITEM) && afterPath != null && afterPath.length() > 0) {
					String filename = URLDecoder.decode(afterPath, "UTF-8");
					File file = new File(filename);
					
					if (mmdb.hasFile(file)) {
						IMixedMediaItem item = mmdb.getByFile(file);
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
							throw new IllegalArgumentException("Failed to retrieve file '"+file.getAbsolutePath()+"' from MMDB desu~.");
						}
					}
					else {
						resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
						resp.setContentType("text/plain");
						resp.getWriter().println("HTTP error 404 file '"+file.getAbsolutePath()+"' not found in MMDB '"+mmdb.getListName()+"' desu~");
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
		
		List<IPlayerLocal> players = PlayerRegister.getLocalPlayers();
		
		for (MediaListReference listRef : MediaFactoryImpl.get().getAllLocalMixedMediaDbs()) {
			dw.startElement("entry");
			printMlistShort(dw, listRef, players);
			dw.endElement("entry");
		}
		
		AbstractFeed.endFeed(dw);
	}
	
	static private void printMlist (HttpServletResponse resp, ILocalMixedMediaDb mmdb, String path, String afterPath) throws IOException, SAXException, MorriganException, DbException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = AbstractFeed.startDocument(resp.getWriter(), "mlist");
		
		if (path == null) {
			printMlistLong(dw, mmdb, false, false);
		}
		else if (path.equals(PATH_ITEMS)) {
			printMlistLong(dw, mmdb, false, true);
		}
		else if (path.equals(PATH_SRC)) {
			printMlistLong(dw, mmdb, true, false);
		}
		else if (path.equals(PATH_ITEM) && afterPath != null && afterPath.length() > 0) {
			String filename = URLDecoder.decode(afterPath, "UTF-8");
			File file = new File(filename);
			if (mmdb.hasFile(file) && file.exists()) {
				ServletHelper.returnFile(file, resp);
			}
		}
		else if (path.equals(PATH_QUERY) && afterPath != null && afterPath.length() > 0) {
			String query = URLDecoder.decode(afterPath, "UTF-8");
			printMlistLong(dw, mmdb, false, true, query);
		}
		else {
			AbstractFeed.addElement(dw, "error", "Unknown path '"+path+"' desu~");
		}
		
		AbstractFeed.endDocument(dw, "mlist");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void printMlistShort (DataWriter dw, MediaListReference listRef, List<IPlayerLocal> players) throws SAXException {
		String fileName = listRef.getIdentifier().substring(listRef.getIdentifier().lastIndexOf(File.separator) + 1);
		
		AbstractFeed.addElement(dw, "title", listRef.getTitle());
		AbstractFeed.addLink(dw, CONTEXTPATH + "/" + ILocalMixedMediaDb.TYPE + "/" + fileName, "self", "text/xml");
		
		for (IPlayerLocal p : players) {
			AbstractFeed.addLink(dw, "/player/" + p.getId() + "/play/" + fileName, "play", "cmd");
		}
	}
	
	static private void printMlistLong (DataWriter dw, ILocalMixedMediaDb ml, boolean listSrcs, boolean listItems) throws SAXException, MorriganException, DbException {
		printMlistLong(dw, ml, listSrcs, listItems, null);
	}
	
	static private void printMlistLong (DataWriter dw, ILocalMixedMediaDb ml, boolean listSrcs, boolean listItems, String queryString) throws SAXException, MorriganException, DbException {
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
    			
    			String file;
    			try {
    				file = URLEncoder.encode(mi.getFilepath(), "UTF-8");
    			} catch (UnsupportedEncodingException e) {
    				throw new RuntimeException(e);
    			}
    			
    			AbstractFeed.addElement(dw, "title", mi.getTitle());
    			
    			StringBuilder sb = new StringBuilder();
    			sb.append(pathToSelf);
    			sb.append("/");
    			sb.append(PATH_ITEM);
    			sb.append("/");
    			sb.append(file);
    			AbstractFeed.addLink(dw, sb.toString(), "self");
    			
    			if (mi.getDateAdded() != null) {
    				AbstractFeed.addElement(dw, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
    			}
    			if (mi.getDateLastModified() != null) {
    				AbstractFeed.addElement(dw, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
    			}
    			AbstractFeed.addElement(dw, "type", mi.getMediaType().getN());
    			AbstractFeed.addElement(dw, "hash", mi.getHashcode());
    			
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
    			
    			dw.endElement("entry");
    		}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
