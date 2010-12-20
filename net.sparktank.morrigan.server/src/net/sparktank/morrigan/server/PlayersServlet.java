package net.sparktank.morrigan.server;

import java.io.IOException;
import java.util.List;

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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final long serialVersionUID = -6463380542721345844L;
	
	private static final String ROOTPATH = "/";
	private static final String NULL = "null";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String actualTarget = req.getRequestURI().substring(req.getContextPath().length());
//		System.err.println("PlayersHandlerXml:target="+actualTarget);
		
		if (actualTarget.equals(ROOTPATH)) {
			try {
				printPlayersList(resp);
			}
			catch (SAXException e) {
				throw new ServletException(e);
			}
		}
		else {
			try {
				int n = Integer.parseInt(actualTarget.substring(ROOTPATH.length()));
				try {
					printPlayer(resp, n);
				}
				catch (IllegalArgumentException e) {
					resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
					resp.setContentType("text/plain");
					resp.getWriter().println("HTTP Error 404 player "+n+" not found desu~");
				}
				catch (SAXException e) {
					throw new ServletException(e);
				}
			}
			catch (NumberFormatException e) {
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				resp.setContentType("text/plain");
				resp.getWriter().println("HTTP Error 404 not found '"+actualTarget+"' desu~");
			}
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("POST to PlayersHandlerXml desu~");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void printPlayersList (HttpServletResponse resp) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = AbstractFeed.startFeed(resp.getWriter());
		
		AbstractFeed.addElement(dw, "title", "Morrigan players desu~");
		AbstractFeed.addLink(dw, CONTEXTPATH, "self", "text/xml");
		
		List<IPlayerLocal> players = PlayerRegister.getLocalPlayers();
		for (IPlayerLocal p : players) {
			dw.startElement("entry");
			printPlayer(dw, p, 0);
			dw.endElement("entry");
		}
		
		AbstractFeed.endFeed(dw);
	}
	
	static private void printPlayer (HttpServletResponse resp, int playerNumber) throws IOException, SAXException {
		IPlayerLocal p = PlayerRegister.getLocalPlayer(playerNumber);
		
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = AbstractFeed.startDocument(resp.getWriter(), "player");
		
		printPlayer(dw, p, 1);
		
		AbstractFeed.endDocument(dw, "player");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void printPlayer (DataWriter dw, IPlayerAbstract p, int detailLevel) throws SAXException {
		if (detailLevel < 0 || detailLevel > 1) throw new IllegalArgumentException("detailLevel must be 0 or 1, not "+detailLevel+".");
		
		String listTitle;
		String listId;
		IMediaTrackList<? extends IMediaTrack> currentList = p.getCurrentList();
		if (currentList != null) {
			listTitle = currentList.getListName();
			listId = currentList.getListId();
		}
		else {
			listTitle = NULL;
			listId = NULL;
		}
		
		String title;
		PlayItem currentItem = p.getCurrentItem();
		if (currentItem != null && currentItem.item != null) {
			title = currentItem.item.getTitle();
		}
		else {
			title = NULL;
		}
		
		int queueLength = p.getQueueList().size();
		DurationData queueDuration = p.getQueueTotalDuration();
		String queueDurationString = (queueDuration.isComplete() ? "" : "more than ") +
				TimeHelper.formatTimeSeconds(queueDuration.getDuration());
		
		AbstractFeed.addElement(dw, "title", "p" + p.getId() + ":" + p.getPlayState().toString() + ":" + title);
		AbstractFeed.addLink(dw, CONTEXTPATH + "/" + p.getId(), "self", "text/xml");
		
		AbstractFeed.addElement(dw, "playerid", p.getId());
		AbstractFeed.addElement(dw, "playstate", p.getPlayState().getN());
		AbstractFeed.addElement(dw, "playorder", p.getPlaybackOrder().getN());
		AbstractFeed.addElement(dw, "queuelength", queueLength);
		AbstractFeed.addElement(dw, "queueduration", queueDurationString);
		AbstractFeed.addElement(dw, "listtitle", listTitle);
		AbstractFeed.addElement(dw, "listid", listId);
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
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
