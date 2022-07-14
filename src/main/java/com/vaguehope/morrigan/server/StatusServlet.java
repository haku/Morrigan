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
 *  GET /status/44e7af9c-99ca-445a-b8a1-a841ade57481
 * </pre>
 */
public class StatusServlet extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String REL_CONTEXTPATH = "/status";
	public static final String CONTEXTPATH = "/" + REL_CONTEXTPATH;

	private static final String ROOTPATH = "/";
	private static final long serialVersionUID = -552170914145385672L;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final AsyncTasksRegister asyncTasksRegister;

	public StatusServlet (final AsyncTasksRegister asyncTasksRegister) {
		this.asyncTasksRegister = asyncTasksRegister;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final String reqPath = ServletHelper.getReqPath(req, REL_CONTEXTPATH);
		try {
			if (reqPath == null || reqPath.length() < 1 || reqPath.equals(ROOTPATH)) {
				printTaskStatusList(resp);
			}
			else {
				final String path = reqPath.substring(ROOTPATH.length()); // Expecting path = '0' or '0/queue'.
				if (path.length() > 0) {
					final AsyncTask task = this.asyncTasksRegister.task(path);
					if (task != null) {
						printTask(resp, task);
					}
					else {
						ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "Task '" + path + "' not found desu~");
					}
				}
				else {
					ServletHelper.error(resp, HttpServletResponse.SC_NOT_FOUND, "Invalid GET request to '" + path + "' desu~");
				}
			}
		}
		catch (final SAXException e) {
			throw new ServletException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void printTaskStatusList (final HttpServletResponse resp) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		final DataWriter dw = FeedHelper.startFeed(resp.getWriter());
		FeedHelper.addElement(dw, "title", "Morrigan task status desu~");
		FeedHelper.addLink(dw, REL_CONTEXTPATH, "self", "text/xml");
		for (final AsyncTask t : this.asyncTasksRegister.tasks()) {
			dw.startElement("entry");
			printTask(dw, t);
			dw.endElement("entry");
		}
		FeedHelper.endFeed(dw);
	}

	private void printTask (final HttpServletResponse resp, final AsyncTask task) throws SAXException, IOException {
		resp.setContentType("text/xml;charset=utf-8");
		final DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "task");
		printTask(dw, task);
		FeedHelper.endDocument(dw, "task");
	}

	private void printTask (final DataWriter dw, final AsyncTask t) throws SAXException {
		FeedHelper.addElement(dw, "id", t.id());
		FeedHelper.addElement(dw, "title", t.title());
		FeedHelper.addElement(dw, "state", t.state().toString());
		FeedHelper.addElement(dw, "subtask", t.subtask());
		FeedHelper.addElement(dw, "lastMsg", t.lastMsg());
		FeedHelper.addElement(dw, "lastErr", t.lastErr());
		FeedHelper.addElement(dw, "progressWorked", t.progressWorked());
		FeedHelper.addElement(dw, "progressTotal", t.progressTotal());
		FeedHelper.addElement(dw, "successful", t.successful());
		FeedHelper.addElement(dw, "summary", t.fullSummary());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
