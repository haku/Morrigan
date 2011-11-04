package com.vaguehope.morrigan.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;
import com.vaguehope.morrigan.asyncui.AsyncProgressRegister;
import com.vaguehope.morrigan.server.feedwriters.AbstractFeed;

/**
 * Valid URLs:
 * <pre>
 *  GET /status
 * </pre>
 */
public class StatusServlet extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String CONTEXTPATH = "/status";
	
	private static final long serialVersionUID = -552170914145385672L;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void doGet (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			printTaskStatusList(resp);
		} catch (SAXException e) {
			throw new ServletException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void printTaskStatusList (HttpServletResponse resp) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = AbstractFeed.startFeed(resp.getWriter());
		
		AbstractFeed.addElement(dw, "title", "Morrigan task status desu~");
		AbstractFeed.addLink(dw, CONTEXTPATH, "self", "text/xml");
		
		String[] reports = AsyncProgressRegister.reportIndiviually();
		for (String r : reports) {
			dw.startElement("entry");
			AbstractFeed.addElement(dw, "summary", r);
			dw.endElement("entry");
		}
		
		AbstractFeed.endFeed(dw);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
