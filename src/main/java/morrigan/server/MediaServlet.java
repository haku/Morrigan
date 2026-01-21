package morrigan.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
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

import morrigan.model.exceptions.MorriganException;
import morrigan.model.media.ListRef;
import morrigan.model.media.ListRefWithTitle;
import morrigan.model.media.MediaFactory;
import morrigan.model.media.MediaItem;
import morrigan.model.media.MediaList;
import morrigan.model.media.MediaNode;
import morrigan.model.media.MediaTag;
import morrigan.model.media.SortColumn;
import morrigan.model.media.MediaItem.MediaType;
import morrigan.model.media.SortColumn.SortDirection;
import morrigan.util.StringHelper;

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
			.registerTypeHierarchyAdapter(ListRefWithTitle.class, new ListRefWithTitleSrl())
			.registerTypeHierarchyAdapter(MediaNode.class, new MediaNodeSrl())
			.registerTypeHierarchyAdapter(MediaItem.class, new MediaItemSrl())
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
		returnJson(resp, this.mediaFactory.allLists());
	}

	private void serveListThings(final PathAndSubPath pth, final HttpServletRequest req, final HttpServletResponse resp)
			throws IOException, MorriganException {
		final ListRef listRef = ListRef.fromUrlForm(pth.getPath());
		final MediaList list = this.mediaFactory.getList(listRef);
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
			serveSearchResults(list, subPth.getSubPath(), req, resp);
		}
		else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	private void serveNodesAndItems(final MediaList list, final String nodeId, final HttpServletResponse resp)
			throws IOException, MorriganException {
		final MediaList node = nodeId != null ? list.makeNode(nodeId, null) : list;
		if (node == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		node.read();
		if (!node.hasNodes()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		final Map<String, Object> ret = new LinkedHashMap<>();
		ret.put("nodeId", node.getNodeId());
		ret.put("title", node.getListName());
		ret.put("nodes", node.getSubNodes());
		ret.put("items", node.getMediaItems());
		returnJson(resp, ret);
	}

	private void serveSearchResults(final MediaList list, final String search, final HttpServletRequest req, final HttpServletResponse resp)
			throws IOException, MorriganException {
		// TODO transcodes
		// TODO specify result limit, including infinite
		final String decoded = URLDecoder.decode(search, StandardCharsets.UTF_8);
		final SortColumn sortColumns = parseSortColumns(req, SortColumn.FILE_PATH);
		final SortDirection sortDirections = parseSortOrder(req, SortDirection.ASC);
		final boolean includeDisabled = ServletHelper.readParamBoolean(req, "includedisabled", false);
		final List<MediaItem> items = list.search(MediaType.TRACK, decoded, MAX_RESULTS, sortColumns, sortDirections, includeDisabled);
		returnJson(resp, items);
	}

	private static void serveItemContent(final MediaList list, final String itemId, final HttpServletRequest req, final HttpServletResponse resp)
			throws IOException, MorriganException {
		final MediaItem item = list.getByFile(itemId);
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

	private static void returnLocalFile(final HttpServletRequest req, final HttpServletResponse resp, final MediaItem item, final File file)
			throws IOException {
		// TODO transcodes
		// TODO resizes
		// TODO check this sets all the right response headers, cos it probably does not.
		ServletHelper.returnFile(file, item.getMimeType(), null, req.getHeader("Range"), resp);
	}

	@SuppressWarnings("resource")
	private static void returnRemoteFile(
			final MediaList list,
			final HttpServletRequest req,
			final HttpServletResponse resp,
			final MediaItem item) throws MorriganException, IOException {
		final long lastModified = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1); // TODO this is fake, should use last modified from remote system.
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

	private class ListRefWithTitleSrl implements JsonSerializer<ListRefWithTitle> {
		@Override
		public JsonElement serialize(final ListRefWithTitle src, final Type typeOfSrc, final JsonSerializationContext context) {
			final JsonObject j = new JsonObject();
			j.addProperty("title", src.getTitle());
			j.addProperty("listRef", src.getListRef().toUrlForm());
			j.addProperty("hasRootNodes", src.getListRef().isHasRootNodes());
			return j;
		}
	}

	private class MediaNodeSrl implements JsonSerializer<MediaNode> {
		@Override
		public JsonElement serialize(final MediaNode src, final Type typeOfSrc, final JsonSerializationContext context) {
			final JsonObject j = new JsonObject();
			j.addProperty("id", src.getId());
			j.addProperty("title", src.getTitle());
			j.addProperty("parentId", src.getParentId());
			return j;
		}
	}

	private class MediaItemSrl implements JsonSerializer<MediaItem> {
		@Override
		public JsonElement serialize(final MediaItem src, final Type typeOfSrc, final JsonSerializationContext context) {
			try {
				final JsonObject j = new JsonObject();
				j.addProperty("id", src.getId());
				j.addProperty("title", src.getTitle());
				j.addProperty("size", src.getFileSize());
				j.addProperty("added", dateToLong(src.getDateAdded()));
				j.addProperty("modified", dateToLong(src.getDateLastModified()));
				j.addProperty("mimetype", src.getMimeType());
				j.addProperty("enabled", src.isEnabled());
				j.addProperty("duration", src.getDuration());
				j.addProperty("starts", src.getStartCount());
				j.addProperty("ends", src.getEndCount());
				j.addProperty("lastplayed", dateToLong(src.getDateLastPlayed()));
				j.add("tags", context.serialize(src.getTags()));
				return j;
			}
			catch (final MorriganException e) {
				throw new SerializationException(e); // TODO come up with a better plan here? like catch and return HTTP 500.
			}
		}
	}

	private class MediaTagSrl implements JsonSerializer<MediaTag> {
		@Override
		public JsonElement serialize(final MediaTag src, final Type typeOfSrc, final JsonSerializationContext context) {
			final JsonObject j = new JsonObject();
			j.add("t", context.serialize(src.getTag()));
			if (src.getClassification() != null) j.add("c", context.serialize(src.getClassification().getClassification()));
			return j;
		}
	}

	private static Long dateToLong(final Date d) {
		return d != null ? d.getTime() : null;
	}

	private static SortColumn parseSortColumns (final HttpServletRequest req, SortColumn defVal) {
		final String raw = StringHelper.downcase(StringHelper.trimToNull(req.getParameter("sort")));
		for (final SortColumn sortColumn : SortColumn.values()) {
			if (sortColumn.name().equalsIgnoreCase(raw)) return sortColumn;
		}
		return defVal;
	}

	private static SortDirection parseSortOrder (final HttpServletRequest req, SortDirection defVal) {
		final String raw = StringHelper.downcase(StringHelper.trimToNull(req.getParameter("order")));
		if ("asc".equals(raw)) {
			return SortDirection.ASC;
		}
		else if ("desc".equals(raw)) {
			return SortDirection.DESC;
		}
		return defVal;
	}
}
