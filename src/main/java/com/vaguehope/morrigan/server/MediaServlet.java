package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;

/* /media
 * /media/somelist
 * /media/somelist/node/node-id
 * /media/somelist/item/item-id
 * /media/somelist/search/item-id
 */
@SuppressWarnings("serial")
public class MediaServlet extends HttpServlet {

	public static final String REL_CONTEXTPATH = "media";
	public static final String CONTEXTPATH = "/" + REL_CONTEXTPATH;

	private final MediaFactory mediaFactory;
	private final Gson gson = new Gson();

	public MediaServlet(final MediaFactory mediaFactory) {
		this.mediaFactory = mediaFactory;
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if (req.getPathInfo() == null || req.getPathInfo().length() == 1) { // null or "/" for media and media/
			serveAllLists(req, resp);
		}
		else {
			serveList(req, resp);
		}
	}

	@SuppressWarnings("resource")
	private void serveAllLists(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		final List<Map<String, String>> lists = new ArrayList<>();

		for (final MediaListReference db : this.mediaFactory.getAllLocalMixedMediaDbs()) {
			lists.add(ImmutableMap.of(
					"name", db.getTitle(),
					"id", db.getIdentifier()));
		}
		for (final MediaListReference el : this.mediaFactory.getExternalLists()) {
			lists.add(ImmutableMap.of(
					"name", el.getTitle(),
					"id", el.getIdentifier()));
		}

		resp.setContentType("application/json");
		resp.getWriter().println(this.gson.toJson(lists));
	}

	private void serveList(final HttpServletRequest req, final HttpServletResponse resp) {
		if (isPath(req, "/node/")) {

		}
		else if (isPath(req, "/item/")) {

		}
		else if (isPath(req, "/search/")) {

		}

	}

	private static boolean isPath(final HttpServletRequest req, final String path) {
		return req.getPathInfo().startsWith(path);
	}

}
