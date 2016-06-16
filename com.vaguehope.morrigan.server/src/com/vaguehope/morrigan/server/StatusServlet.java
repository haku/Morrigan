package com.vaguehope.morrigan.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;
import com.vaguehope.morrigan.server.util.FeedHelper;
import com.vaguehope.morrigan.tasks.AsyncTask;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;

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

	private final AsyncTasksRegister asyncTasksRegister;

	public StatusServlet (final AsyncTasksRegister asyncTasksRegister) {
		this.asyncTasksRegister = asyncTasksRegister;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			printTaskStatusList(resp);
		} catch (SAXException e) {
			throw new ServletException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void printTaskStatusList (final HttpServletResponse resp) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		DataWriter dw = FeedHelper.startFeed(resp.getWriter());

		FeedHelper.addElement(dw, "title", "Morrigan task status desu~");
		FeedHelper.addLink(dw, CONTEXTPATH, "self", "text/xml");

		for (AsyncTask t : this.asyncTasksRegister.tasks()) {
			dw.startElement("entry");
			FeedHelper.addElement(dw, "id", t.id());
			FeedHelper.addElement(dw, "title", t.title());
			FeedHelper.addElement(dw, "state", t.state().toString());
			FeedHelper.addElement(dw, "subtask", t.subtask());
			FeedHelper.addElement(dw, "lastMsg", t.lastMsg());
			FeedHelper.addElement(dw, "lastErr", t.lastErr());
			FeedHelper.addElement(dw, "progressWorked", t.progressWorked());
			FeedHelper.addElement(dw, "progressTotal", t.progressTotal());
			FeedHelper.addElement(dw, "successful", t.successful());
			FeedHelper.addElement(dw, "summary", t.summary());
			dw.endElement("entry");
		}

		FeedHelper.endFeed(dw);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
