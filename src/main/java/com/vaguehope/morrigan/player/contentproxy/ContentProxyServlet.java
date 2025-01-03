package com.vaguehope.morrigan.player.contentproxy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.morrigan.player.contentproxy.TransientContentIds.TransientContentItem;

public class ContentProxyServlet extends HttpServlet {

	public static String PATH_PREFIX = "proxy/";

	private static final long serialVersionUID = 1756070670841835037L;
	private final TransientContentIds transientContentIds;

	public ContentProxyServlet(final TransientContentIds transientContentIds) {
		this.transientContentIds = transientContentIds;
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final String pathId = StringUtils.removeStart(req.getPathInfo(), "/");
		final TransientContentItem item = this.transientContentIds.resolve(pathId);
		if (item == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid ID: " + pathId);
			return;
		}

		item.contentServer.doGet(req, resp, item.listId, item.itemId);
	}

}
