package net.sparktank.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import net.sparktank.morrigan.player.PlayerRegister;
import net.sparktank.morrigan.server.feedwriters.AbstractFeed;
import net.sparktank.morrigan.server.feedwriters.XmlHelper;
import net.sparktank.sqlitewrapper.DbException;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;

public class MlistServlet extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	

	public static final String CONTEXTPATH = "/mlist";
	
	private static final String PATH_ITEMS = "items";
	private static final String PATH_SRC = "src";
	
	public static final String CMD_NEWMMDB = "newmmdb";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final long serialVersionUID = 2754601524882233866L;
	
	private static final String ROOTPATH = "/";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String actualTarget = req.getRequestURI().substring(req.getContextPath().length());
		try {
			writeResponse(resp, actualTarget);
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
		resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		resp.setContentType("text/plain");
		resp.getWriter().println("POST not yet implemented desu~");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void writeResponse (HttpServletResponse resp, String actualTarget) throws IOException, ServletException, DbException, SAXException, MorriganException {
		if (actualTarget.equals(ROOTPATH)) {
			try {
				printMlistList(resp);
			} catch (SAXException e) {
				throw new ServletException(e);
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
						printMlist(resp, mmdb, subPath);
					}
					else {
						System.err.println("Unknown type '"+type+"'.");
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
	
	static private void printMlist (HttpServletResponse resp, ILocalMixedMediaDb mmdb, String path) throws IOException, SAXException, MorriganException {
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
	
	static private void printMlistLong (DataWriter dw, ILocalMixedMediaDb ml, boolean listSrcs, boolean listItems) throws SAXException, MorriganException {
		ml.read();
		
		String listFile;
		try {
			listFile = URLEncoder.encode(AbstractFeed.filenameFromPath(ml.getListId()), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		dw.dataElement("title", ml.getListName());
		dw.dataElement("count", String.valueOf(ml.getCount()));
		
		DurationData totalDuration = ml.getTotalDuration();
		dw.dataElement("duration", String.valueOf(totalDuration.getDuration()));
		dw.dataElement("durationcomplete", String.valueOf(totalDuration.isComplete()));
		
		AbstractFeed.addLink(dw, CONTEXTPATH + "/" + ml.getType() + "/" + listFile, "self", "text/xml");
		if (!listItems) AbstractFeed.addLink(dw, CONTEXTPATH + "/" + ml.getType() + "/" + listFile + "/" + PATH_ITEMS, PATH_ITEMS, "text/xml");
		AbstractFeed.addLink(dw, CONTEXTPATH + "/" + ml.getType() + "/" + listFile + "/" + PATH_SRC, PATH_SRC, "text/xml");
		
		if (listSrcs) {
			List<String> src;
			src = ml.getSources();
			
			for (String s : src) {
				AbstractFeed.addElement(dw, "src", s);
			}
		}
		
		if (listItems) {
    		for (IMixedMediaItem mi : ml.getMediaItems()) {
    			dw.startElement("entry");
    			
    			String file;
    			try {
    				file = URLEncoder.encode(mi.getFilepath(), "UTF-8");
    			} catch (UnsupportedEncodingException e) {
    				throw new RuntimeException(e);
    			}
    			
    			AbstractFeed.addElement(dw, "title", mi.getTitle());
    			AbstractFeed.addLink(dw, CONTEXTPATH + "/" + ml.getType() + "/" + AbstractFeed.filenameFromPath(ml.getListId()) + "/" + file, "self", "text/xml");
    			if (mi.getDateAdded() != null) {
    				AbstractFeed.addElement(dw, "dateadded", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateAdded()));
    			}
    			if (mi.getDateLastModified() != null) {
    				AbstractFeed.addElement(dw, "datelastmodified", XmlHelper.getIso8601UtcDateFormatter().format(mi.getDateLastModified()));
    			}
    			AbstractFeed.addElement(dw, "type", mi.getMediaType().getN());
    			AbstractFeed.addElement(dw, "hash", mi.getHashcode());
    			
    			if (mi.getMediaType() == MediaType.TRACK) {
    				if (mi.isPlayable()) {
    					AbstractFeed.addLink(dw, "/player/0/play/" + listFile + "/" + file, "play", "cmd"); // FIXME list all players here.
    				}
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
