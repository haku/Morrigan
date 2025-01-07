package com.vaguehope.morrigan.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.SerializationException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.MediaNode;
import com.vaguehope.morrigan.model.media.MediaTag;

/* GET /media                               all lists
 * GET /media/somelist                      root node, nodes and items
 * GET /media/somelist/node/node-id         sub node, nodes and items
 * GET /media/somelist/item/item-id         item file content
 * GET /media/somelist/item/dir/a/file.ext  item file content
 * GET /media/somelist/search/query-string  list of items
 */
@SuppressWarnings("serial")
public class MediaServlet extends HttpServlet {

	public static final String REL_CONTEXTPATH = "media";
	public static final String CONTEXTPATH = "/" + REL_CONTEXTPATH;

	private static final int MAX_RESULTS = 500;

	private final MediaFactory mediaFactory;
	private final Gson gson = new GsonBuilder()
			.registerTypeHierarchyAdapter(MediaListReference.class, new MediaListReferenceSrl())
			.registerTypeHierarchyAdapter(MediaNode.class, new MediaNodeSrl())
			.registerTypeHierarchyAdapter(IMediaItem.class, new MediaItemSrl())
			.registerTypeHierarchyAdapter(MediaTag.class, new MediaTagSrl())
			.create();

	public MediaServlet(final MediaFactory mediaFactory) {
		this.mediaFactory = mediaFactory;
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			final PathAndSubPath pth = PathAndSubPath.split(req.getPathInfo());
			if (!pth.hasPath()) {
				serveAllLists(req, resp);
			}
			else {
				serveListThings(pth, req, resp);
			}
		}
		catch (final MorriganException e) {
			throw new ServletException(e);
		}
	}

	private void serveAllLists(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
		final List<Object> ret = new ArrayList<>();
		ret.addAll(this.mediaFactory.getAllLocalMixedMediaDbs());
		ret.addAll(this.mediaFactory.getExternalLists());
		returnJson(resp, ret);
	}

	private void serveListThings(final PathAndSubPath pth, final HttpServletRequest req, final HttpServletResponse resp)
			throws IOException, MorriganException {
		final IMediaItemList list = this.mediaFactory.getMediaListByMid(pth.getPath(), null);
		if (list == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (!pth.hasSubPath()) {
			serveNodesAndItems(list, null, resp);
			return;
		}

		final PathAndSubPath subPth = PathAndSubPath.split(pth.getSubPath());
		if (!subPth.hasSubPath()) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
		else if (subPth.pathIs("item")) {
			serveItemContent(list, subPth.getSubPath(), req, resp);
		}
		else if (subPth.pathIs("node")) {
			serveNodesAndItems(list, subPth.getSubPath(), resp);
		}
		else if (subPth.pathIs("search")) {
			serveSearchResults(list, subPth.getSubPath(), resp);
		}
		else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private void serveNodesAndItems(final IMediaItemList list, final String nodeId, final HttpServletResponse resp) throws IOException, MorriganException {
		final IMediaItemList node = nodeId != null ? list.makeNode(nodeId, null) : list;
		if (node == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (!node.hasNodes()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		final Map<String, Object> ret = new HashMap<>();
		ret.put("nodes", node.getSubNodes());
		ret.put("items", node.getMediaItems());
		returnJson(resp, ret);
	}

	private void serveSearchResults(final IMediaItemList list, final String search, final HttpServletResponse resp) {
		throw new UnsupportedOperationException();
	}

	private static void serveItemContent(final IMediaItemList list, final String itemId, final HttpServletRequest req, final HttpServletResponse resp) throws IOException, MorriganException {
		final IMediaItem item = list.getByFile(itemId);
		if (item == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		final File file = item.getFile();
		if (file != null) {
			if (ServletHelper.checkCanReturn304(file.lastModified(), req, resp)) return;
			if (file.exists()) {
				returnLocalFile(req, resp, item, file);
			}
			else {
				resp.sendError(HttpServletResponse.SC_GONE);
			}
			return;
		}

		returnRemoteFile(list, req, resp, item);
	}

	private static void returnLocalFile(final HttpServletRequest req, final HttpServletResponse resp, final IMediaItem item, final File file) throws IOException {
		// TODO transcodes
		// TODO resizes
		// TODO check this sets all the right response headers, cos it probably does not.
		ServletHelper.returnFile(file, item.getMimeType(), null, req.getHeader("Range"), resp);
	}

	@SuppressWarnings("resource")
	private static void returnRemoteFile(final IMediaItemList list, final HttpServletRequest req, final HttpServletResponse resp, final IMediaItem item) throws MorriganException, IOException {
		final long lastModified = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1); // TODO this is a total fake, should use last modified from remote system.
		if (ServletHelper.checkCanReturn304(lastModified, req, resp)) return;
		// TODO pass through length and modified date?
		ServletHelper.prepForReturnFile(0, System.currentTimeMillis(), item.getMimeType(), null, resp);
		// TODO some sort of check this is supported?
		list.copyItemFile(item, resp.getOutputStream());
		resp.flushBuffer();
	}

	@SuppressWarnings("resource")
	private void returnJson(final HttpServletResponse resp, final Object ret) throws IOException {
		resp.setContentType("application/json");
		resp.getWriter().println(this.gson.toJson(ret));
	}

	public class MediaListReferenceSrl implements JsonSerializer<MediaListReference> {
		@Override
		public JsonElement serialize(final MediaListReference src, final Type typeOfSrc, final JsonSerializationContext context) {
			final JsonObject j = new JsonObject();
			j.add("name", context.serialize(src.getTitle()));
			j.add("mid", context.serialize(src.getMid().replace("/", ":")));
			return j;
		}
	}

	public class MediaNodeSrl implements JsonSerializer<MediaNode> {
		@Override
		public JsonElement serialize(final MediaNode src, final Type typeOfSrc, final JsonSerializationContext context) {
			final JsonObject j = new JsonObject();
			j.add("id", context.serialize(src.getId()));
			j.add("title", context.serialize(src.getTitle()));
			j.add("parentId", context.serialize(src.getParentId()));
			return j;
		}
	}

	public class MediaItemSrl implements JsonSerializer<IMediaItem> {
		@Override
		public JsonElement serialize(final IMediaItem src, final Type typeOfSrc, final JsonSerializationContext context) {
			try {
				final JsonObject j = new JsonObject();
				j.add("id", context.serialize(src.getRemoteId())); // TODO how does this work for local items?
				j.add("title", context.serialize(src.getTitle()));
				j.add("size", context.serialize(src.getFileSize()));
				j.add("added", context.serialize(src.getDateAdded()));
				j.add("modified", context.serialize(src.getDateLastModified()));
				j.add("mimetype", context.serialize(src.getMimeType()));
				j.add("enabled", context.serialize(src.isEnabled()));
				j.add("starts", context.serialize(src.getStartCount()));
				j.add("ends", context.serialize(src.getEndCount()));
				j.add("lastplayed", context.serialize(src.getDateLastPlayed()));
				j.add("tags", context.serialize(src.getTags()));
				return j;
			}
			catch (final MorriganException e) {
				throw new SerializationException(e);  // TODO come up with a better plan here?  like catch and return HTTP 500.
			}
		}
	}

	public class MediaTagSrl implements JsonSerializer<MediaTag> {
		@Override
		public JsonElement serialize(final MediaTag src, final Type typeOfSrc, final JsonSerializationContext context) {
			final JsonObject j = new JsonObject();
			j.add("t", context.serialize(src.getTag()));
			if (src.getClassification() != null) j.add("c", context.serialize(src.getClassification().getClassification()));
			return j;
		}
	}

}
