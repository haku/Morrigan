package com.vaguehope.morrigan.dlna.content;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.DIDLObject.Property.DC;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.VideoItem;
import org.seamless.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.MediaFormat;
import com.vaguehope.morrigan.dlna.UpnpHelper;
import com.vaguehope.morrigan.dlna.httpserver.MediaServer;
import com.vaguehope.morrigan.dlna.util.Cache;
import com.vaguehope.morrigan.dlna.util.HashHelper;
import com.vaguehope.morrigan.dlna.util.LruMap;
import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMixedMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class ContentAdaptor {

	private static final int MAX_CACHE_AGE_SECONDS = 60;
	private static final int MAX_TAGS = 250;
	private static final int MAX_ITEMS = 1000;

	private static final Logger LOG = LoggerFactory.getLogger(ContentAdaptor.class);

	private final MediaFactory mediaFactory;
	private final MediaServer mediaServer;
	private final MediaFileLocator mediaFileLocator;
	private final DbHelper dbHelper;

	private final Map<String, ContentNode> cache = Collections.synchronizedMap(new LruMap<String, ContentNode>(100, 100));

	private final Map<String, MediaListReference> objectIdToMediaListReference = new ConcurrentHashMap<String, MediaListReference>();
	private final Map<String, MlrAnd<DbSubNodeType>> objectIdToDbSubNodeType = new ConcurrentHashMap<String, MlrAnd<DbSubNodeType>>();
	private final Map<String, MlrAnd<MediaTag>> objectIdToTag = new ConcurrentHashMap<String, MlrAnd<MediaTag>>();
	private final Map<String, MlrAnd<MediaAlbum>> objectIdToAlbum = new ConcurrentHashMap<String, MlrAnd<MediaAlbum>>();

	public ContentAdaptor (final MediaFactory mediaFactory, final MediaServer mediaServer, final MediaFileLocator mediaFileLocator) {
		this.mediaFactory = mediaFactory;
		this.mediaServer = mediaServer;
		this.mediaFileLocator = mediaFileLocator;
		this.dbHelper = new DbHelper(mediaFactory);
	}

	/**
	 * Returns null if unknown objectId.
	 */
	public ContentNode getNode (final String objectId, final boolean firstPage) throws DbException, MorriganException {
		final ContentNode cached = this.cache.get(objectId);
		if (cached != null && (!firstPage || cached.age(TimeUnit.SECONDS) < MAX_CACHE_AGE_SECONDS)) return cached;

		final ContentNode made = makeNode(objectId);
		this.cache.put(objectId, made);
		return made;
	}

	private ContentNode makeNode (final String objectId) throws DbException, MorriganException {
		if (ContentGroup.ROOT.getId().equals(objectId)) {
			return makeRootNode();
		}

		// TODO make more efficient by checking id prefixes?

		{
			final MediaListReference mlr = this.objectIdToMediaListReference.get(objectId);
			if (mlr != null) {
				return makeMediaListNode(objectId, mlr);
			}
		}

		{
			final MlrAnd<DbSubNodeType> mlrAndDbSubNodeType = this.objectIdToDbSubNodeType.get(objectId);
			if (mlrAndDbSubNodeType != null) {
				return makeDbSubNode(objectId, mlrAndDbSubNodeType.getMlr(), mlrAndDbSubNodeType.getObj());
			}
		}

		{
			final MlrAnd<MediaTag> mlrAndTag = this.objectIdToTag.get(objectId);
			if (mlrAndTag != null) {
				return makeTagNode(objectId, mlrAndTag.getMlr(), mlrAndTag.getObj());
			}
		}

		{
			final MlrAnd<MediaAlbum> mlrAndAlbum = this.objectIdToAlbum.get(objectId);
			if (mlrAndAlbum != null) {
				return makeAlbumNode(objectId, mlrAndAlbum.getMlr(), mlrAndAlbum.getObj());
			}
		}

		LOG.info("Not found: {}", objectId);
		return null;
	}

	public IMixedMediaDb objectIdToDb (final String objectId) throws DbException, MorriganException {
		final MediaListReference mlr = this.objectIdToMediaListReference.get(objectId);
		if (mlr == null) return null;
		return this.dbHelper.mediaListReferenceToDb(mlr);
	}

	private ContentNode makeRootNode () {
		final Container c = new Container();
		c.setClazz(new DIDLObject.Class("object.container"));
		c.setId(ContentGroup.ROOT.getId());
		c.setParentID("-1");
		c.setTitle(ContentGroup.ROOT.getHumanName());
		c.setCreator(MediaServerDeviceFactory.METADATA_MODEL_NAME);
		c.setRestricted(true);
		c.setSearchable(false); // Root is not searchable.
		c.setWriteStatus(WriteStatus.NOT_WRITABLE);

		for (final MediaListReference mlr : this.mediaFactory.getAllLocalMixedMediaDbs()) {
			final Container mlc = makeContainer(ContentGroup.ROOT.getId(), localMmdbObjectId(mlr), mlr.getTitle());
			mlc.setSearchable(true); // Each DB is searchable.
			c.addContainer(mlc);
		}
		updateContainer(c);

		return new ContentNode(c);
	}

	private ContentNode makeMediaListNode (final String objectId, final MediaListReference mlr) {
		final Container c = makeContainer(ContentGroup.ROOT.getId(), objectId, mlr.getTitle());

		for (final DbSubNodeType t : DbSubNodeType.values()) {
			c.addContainer(makeContainer(objectId, dbSubNodeObjectId(mlr, t), t.getTitle()));
		}

		return new ContentNode(c);
	}

	private static enum DbSubNodeType {
		TAGS("Tags"),
		ALBUMS("Albums"),
		RECENTLY_ADDED("Recently Added"),
		MOST_PLAYED("Most Played");

		private final String title;

		private DbSubNodeType (final String title) {
			this.title = title;
		}

		public String getTitle () {
			return this.title;
		}
	}

	private ContentNode makeDbSubNode (final String objectId, final MediaListReference mlr, final DbSubNodeType type) throws DbException, MorriganException {
		final IMixedMediaDb db = this.dbHelper.mediaListReferenceToDb(mlr);
		if (db != null) {
			switch (type) {
				case TAGS:
					return makeDbTagsNode(objectId, mlr, db);
				case ALBUMS:
					return makeDbAlbumsNode(objectId, mlr, db);
				case RECENTLY_ADDED:
					return makeDbRecentlyAddedNode(objectId, mlr, db);
				case MOST_PLAYED:
					return makeDbMostPlayedNode(objectId, mlr, db);
				default:
					return null;
			}
		}
		throw new IllegalArgumentException("Unknown DB type: " + mlr);
	}

	private ContentNode makeDbTagsNode (final String objectId, final MediaListReference mlr, final IMixedMediaDb db) throws MorriganException {
		final Container c = makeContainer(localMmdbObjectId(mlr), objectId, mlr.getTitle());

		for (final MediaTag tag : db.getTopTags(MAX_TAGS)) {
			c.addContainer(makeContainer(objectId, tagObjectId(mlr, tag), tag.getTag()));
		}
		updateContainer(c);

		return new ContentNode(c);
	}

	private ContentNode makeTagNode (final String objectId, final MediaListReference mlr, final MediaTag tag) throws DbException, MorriganException {
		final IMixedMediaDb db = this.dbHelper.mediaListReferenceToDb(mlr);
		if (db != null) return makeDbTagNode(objectId, mlr, db, tag);
		throw new IllegalArgumentException("Unknown DB type: " + mlr);
	}

	private ContentNode makeDbTagNode (final String objectId, final MediaListReference mlr, final IMixedMediaDb db, final MediaTag tag) throws DbException, MorriganException {
		return queryToContentNode(dbSubNodeObjectId(mlr, DbSubNodeType.TAGS), objectId, mlr, db,
				String.format("t=\"%s\"", tag.getTag()),
				new IDbColumn[] {
						IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_ENDCNT,
						IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_DADDED,
						IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE
				},
				new SortDirection[] { SortDirection.DESC, SortDirection.ASC, SortDirection.ASC });
	}

	private ContentNode makeDbAlbumsNode (final String objectId, final MediaListReference mlr, final IMixedMediaDb db) throws MorriganException {
		final Container c = makeContainer(localMmdbObjectId(mlr), objectId, mlr.getTitle());

		for (final MediaAlbum album : db.getAlbums()) {
			final Container albumC = makeContainer(objectId, albumObjectId(mlr, album), album.getName());

			final File artFile = db.findAlbumCoverArt(album);
			if (artFile != null) {
				final Res artRes = makeArtRes(artFile, this.mediaFileLocator.albumArtId(mlr, album));
				if (artRes != null) albumC.addProperty(new DIDLObject.Property.UPNP.ALBUM_ART_URI(URI.create(artRes.getValue())));
			}

			c.addContainer(albumC);
		}
		updateContainer(c);

		return new ContentNode(c);
	}

	private ContentNode makeAlbumNode (final String objectId, final MediaListReference mlr, final MediaAlbum album) throws DbException, MorriganException {
		final IMixedMediaDb db = this.dbHelper.mediaListReferenceToDb(mlr);
		if (db != null) return makeDbAlbumNode(objectId, mlr, db, album);
		throw new IllegalArgumentException("Unknown DB type: " + mlr);
	}

	private ContentNode makeDbAlbumNode (final String objectId, final MediaListReference mlr, final IMixedMediaDb db, final MediaAlbum album) throws MorriganException {
		final Container c = makeContainer(dbSubNodeObjectId(mlr, DbSubNodeType.ALBUMS), objectId, mlr.getTitle());
		addItemsToContainer(mlr, c, db, db.getAlbumItems(MediaType.TRACK, album));
		return new ContentNode(c);
	}

	private ContentNode makeDbRecentlyAddedNode (final String objectId, final MediaListReference mlr, final IMixedMediaDb db) throws DbException, MorriganException {
		return queryToContentNode(localMmdbObjectId(mlr), objectId, mlr, db,
				"*",
				new IDbColumn[] {
						IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_DADDED,
						IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE
				},
				new SortDirection[] { SortDirection.DESC, SortDirection.ASC });
	}

	private ContentNode makeDbMostPlayedNode (final String objectId, final MediaListReference mlr, final IMixedMediaDb db) throws DbException, MorriganException {
		return queryToContentNode(localMmdbObjectId(mlr), objectId, mlr, db,
				"*",
				new IDbColumn[] {
						IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_ENDCNT,
						IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE
				},
				new SortDirection[] { SortDirection.DESC, SortDirection.ASC });
	}

	private ContentNode queryToContentNode (final String parentObjectId, final String objectId, final MediaListReference mlr,
			final IMixedMediaDb db, final String term, final IDbColumn[] sortColumns, final SortDirection[] sortDirections) throws DbException, MorriganException {
		final Container c = makeContainer(parentObjectId, objectId, mlr.getTitle());
		addItemsToContainer(mlr, c, db, db.simpleSearchMedia(MediaType.TRACK, term, MAX_ITEMS, sortColumns, sortDirections, false));
		return new ContentNode(c);
	}

	private void addItemsToContainer (final MediaListReference mlr, final Container c, final IMixedMediaDb db, final Collection<IMixedMediaItem> items) throws MorriganException {
		for (final IMixedMediaItem item : items) {
			final Item i = makeItem(c, mlr, item);
			if (i != null) {
				tagsToDescription(db, item, i);
				c.addItem(i);
			}
		}
		updateContainer(c);
	}

	private final Cache<String, List<IMixedMediaItem>> queryCache = new Cache<String, List<IMixedMediaItem>>(50);

	public List<Item> queryToItems(final MediaListReference mlr, final IMixedMediaDb db, final String term, final Container parentContainer, final int maxResults) throws DbException, MorriganException {
		final String cacheKey = String.format("%s|%s|%s", term, maxResults, mlr.getIdentifier());
		List<IMixedMediaItem> results = this.queryCache.getFresh(cacheKey, 1, TimeUnit.MINUTES);
		if (results == null) {
			results = db.simpleSearchMedia(MediaType.TRACK, term, maxResults);
			this.queryCache.put(cacheKey, results);
		}

		final List<Item> ret = new ArrayList<Item>();
		for (final IMixedMediaItem item : results) {
			final Item i = makeItem(parentContainer, mlr, item);
			if (i != null) {
				tagsToDescription(db, item, i);
				ret.add(i);
			}
		}
		return ret;
	}

	private Item makeItem (final Container parentContainer, final MediaListReference mlr, final IMixedMediaItem mediaItem) {
		final File file = new File(mediaItem.getFilepath());
		final MediaFormat format = MediaFormat.identify(file);
		if (format == null) {
			LOG.warn("Unknown media format: {}", file.getAbsolutePath());
			return null;
		}

		final String objectId = this.mediaFileLocator.mediaItemId(mlr, mediaItem);
		final String uri = this.mediaServer.uriForId(objectId);
		final Res res = new Res(format.toMimeType(), Long.valueOf(file.length()), uri);
		res.setSize(file.length());

		final int durationSeconds = mediaItem.getDuration();
		if (durationSeconds > 0) res.setDuration(ModelUtil.toTimeString(durationSeconds));

		if (mediaItem.getWidth() > 0 || mediaItem.getHeight() > 0) {
			res.setResolution(mediaItem.getWidth(), mediaItem.getHeight());
		}

		final Item item;
		switch (format.getContentGroup()) {
			case VIDEO:
				item = new VideoItem(objectId, parentContainer, mediaItem.getTitle(), "", res);
//				findSubtitles(file, format, item); // TODO
				break;
			case IMAGE:
				item = new ImageItem(objectId, parentContainer, mediaItem.getTitle(), "", res);
				break;
			case AUDIO:
				item = new AudioItem(objectId, parentContainer, mediaItem.getTitle(), "", res);
				break;
			default:
				throw new IllegalArgumentException();
		}

		item.addProperty(new DIDLObject.Property.DC.DATE(UpnpHelper.DC_DATE_FORMAT.get().format(mediaItem.getDateAdded())));

		final Res artRes = makeArtRes(mediaItem.findCoverArt(), this.mediaFileLocator.mediaItemArtId(mlr, mediaItem));
		if (artRes != null) item.addResource(artRes);

		return item;
	}

	private static void tagsToDescription (final IMixedMediaDb db, final IMixedMediaItem mediaItem, final Item item) throws MorriganException {
		final List<MediaTag> tags = db.getTags(mediaItem);
		if (tags != null && tags.size() > 0) {
			StringBuilder s = null;
			for (final MediaTag tag : tags) {
				if (tag.getType() != MediaTagType.MANUAL) continue;
				if (s == null) {
					s = new StringBuilder("tags: ");
				}
				else {
					s.append(", ");
				}
				s.append(tag.getTag());
			}
			if (s != null) item.replaceFirstProperty(new DC.DESCRIPTION(s.toString()));
		}
	}

	private Res makeArtRes (final File artFile, final String id) {
		if (artFile == null) return null;

		final MediaFormat artFormat = MediaFormat.identify(artFile);
		if (artFormat == null) {
			LOG.warn("Ignoring art file of unsupported type: {}", artFile);
			return null;
		}
		final MimeType artMimeType = artFormat.toMimeType();

		final String artUri = this.mediaServer.uriForId(id);
		return new Res(artMimeType, Long.valueOf(artFile.length()), artUri);
	}

	private String localMmdbObjectId (final MediaListReference mlr) {
		final String id = makeLocalMmdbObjectId(mlr);
		this.objectIdToMediaListReference.put(id, mlr);
		return id;
	}

	private String dbSubNodeObjectId (final MediaListReference mlr, final DbSubNodeType t) {
		final String id = makeDbSubNodeObjectId(mlr, t);
		this.objectIdToDbSubNodeType.put(id, new MlrAnd<DbSubNodeType>(mlr, t));
		return id;
	}

	private String tagObjectId (final MediaListReference mlr, final MediaTag tag) {
		final String id = makeTagObjectId(mlr, tag);
		this.objectIdToTag.put(id, new MlrAnd<MediaTag>(mlr, tag));
		return id;
	}

	private String albumObjectId (final MediaListReference mlr, final MediaAlbum album) {
		final String id = makeAlbumObjectId(mlr, album);
		this.objectIdToAlbum.put(id, new MlrAnd<MediaAlbum>(mlr, album));
		return id;
	}

	private static String safeName (final String s) {
		return s.replaceAll("[^a-zA-Z0-9]", "_");
	}

	private static String makeLocalMmdbObjectId (final MediaListReference mlr) {
		return String.format("ldb-%s-%s", safeName(mlr.getIdentifier()), HashHelper.sha1(mlr.getIdentifier()));
	}

	private static String makeDbSubNodeObjectId (final MediaListReference mlr, final DbSubNodeType t) {
		return String.format("dsn-%s-%s", safeName(mlr.getIdentifier()), safeName(t.name()));
	}

	private static String makeTagObjectId (final MediaListReference mlr, final MediaTag tag) {
		return String.format("tag-%s-%s", safeName(tag.getTag()),
				HashHelper.sha1(String.format("%s-%s-%s", mlr.getIdentifier(), tag.getClassification(), tag.getTag())));
	}

	private static String makeAlbumObjectId (final MediaListReference mlr, final MediaAlbum album) {
		return String.format("alb-%s-%s", safeName(album.getName()),
				HashHelper.sha1(String.format("%s-%s", mlr.getIdentifier(), album.getName())));
	}

	private static Container makeContainer (final String parentContainerId, final String id, final String title) {
		final Container c = new Container();
		c.setClazz(new DIDLObject.Class("object.container"));
		c.setId(id);
		c.setParentID(parentContainerId);
		c.setTitle(title);
		c.setRestricted(true);
		c.setWriteStatus(WriteStatus.NOT_WRITABLE);
		updateContainer(c);
		return c;
	}

	private static void updateContainer (final Container container) {
		container.setChildCount(Integer.valueOf(container.getContainers().size() + container.getItems().size()));
	}

}
