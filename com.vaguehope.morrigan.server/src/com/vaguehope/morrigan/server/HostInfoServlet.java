package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xml.sax.SAXException;

import com.megginson.sax.DataWriter;
import com.vaguehope.morrigan.server.util.FeedHelper;

/**
 * Valid URLs:
 *
 * <pre>
 * GET / hostinfo
 * </pre>
 */
public class HostInfoServlet extends HttpServlet {

	public static final String CONTEXTPATH = "/hostinfo";

	private static final long serialVersionUID = -2644270109117823836L;

	private final String hostName;

	public HostInfoServlet () {
		this.hostName = findHostName();
	}

	@Override
	protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			printHostInfo(resp);
		}
		catch (final SAXException e) {
			throw new ServletException(e);
		}
	}

	private void printHostInfo (final HttpServletResponse resp) throws IOException, SAXException {
		resp.setContentType("text/xml;charset=utf-8");
		final DataWriter dw = FeedHelper.startDocument(resp.getWriter(), "hostinfo");

		FeedHelper.addElement(dw, "hostname", this.hostName);

		FeedHelper.endDocument(dw, "hostinfo");
	}

	private static String findHostName () {
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (final UnknownHostException e) {
			return "";
		}
	}

}
