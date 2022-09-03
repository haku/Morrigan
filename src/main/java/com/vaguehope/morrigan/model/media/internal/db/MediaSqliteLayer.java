package com.vaguehope.morrigan.model.media.internal.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.helper.EqualHelper;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayerChangeListener;
import com.vaguehope.morrigan.model.media.MatchMode;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.internal.MediaAlbumImpl;
import com.vaguehope.morrigan.model.media.internal.MediaTagClassificationFactory;
import com.vaguehope.morrigan.model.media.internal.MediaTagImpl;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.sqlitewrapper.GenericSqliteLayer;

public abstract class MediaSqliteLayer<T extends IMediaItem> extends GenericSqliteLayer implements IMediaItemStorageLayer<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.

	protected MediaSqliteLayer (final String dbFilePath, final boolean autoCommit) throws DbException {
		super(dbFilePath, autoCommit);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Collection<IMediaItemStorageLayerChangeListener<T>> changeListeners = Collections.synchronizedList(new ArrayList<IMediaItemStorageLayerChangeListener<T>>());
	private final IMediaItemStorageLayerChangeListener<T> changeCaller = new IMediaItemStorageLayerChangeListenerAdaptor<>(this.changeListeners);

	@Override
	public void addChangeListener(final IMediaItemStorageLayerChangeListener<T> listener) {
		// TODO rewrite this to use a map instead?
		if (!this.changeListeners.contains(listener)) this.changeListeners.add(listener);
	}

	@Override
	public void removeChangeListener(final IMediaItemStorageLayerChangeListener<T> listener) {
		this.changeListeners.remove(listener);
	}


	@Override
	public IMediaItemStorageLayerChangeListener<T> getChangeEventCaller () {
		return this.changeCaller;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public methods for persisted props.

	@Override
	public void setProp (final String key, final String value) throws DbException {
		try {
			local_setProp(key, value);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public String getProp (final String key) throws DbException {
		try {
			return local_getProp(key);
		} catch (IllegalArgumentException e) {
			return null;
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public Map<String, String> getProps () throws DbException {
		try {
			return local_getProps();
		} catch (IllegalArgumentException e) {
			return null;
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public methods for tags.

	@Override
	public List<MediaTag> getTopTags (final int countLimit) throws DbException {
		try {
			return local_getTopTags(countLimit);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public Map<String, MediaTag> tagSearch (final String query, MatchMode mode, final int resLimit) throws DbException {
		try {
			return local_tagSearch(query, mode, resLimit);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean hasTagsIncludingDeleted (final IDbItem item) throws DbException {
		try {
			return local_hasTagsIncludingDeleted(item.getDbRowId());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean hasTag (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc) throws DbException {
		try {
			for (final MediaTag t : local_hasTag(item.getDbRowId(), tag, type, mtc)) {
				if (!t.isDeleted()) return true;
			}
			return false;
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<MediaTag> getTags (final IDbItem item, final boolean includeDelete) throws DbException {
		try {
			return local_getTags(item.getDbRowId(), includeDelete);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<MediaTagClassification> getTagClassifications () throws DbException {
		try {
			return local_getTagClassifications();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean addTag (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc) throws DbException {
		try {
			return local_addTag(item, tag, type, mtc);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean addTag (final IDbItem item, final String tag, final MediaTagType type, final String mtc) throws DbException {
		try {
			return local_addTag(item, tag, type, mtc);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean addTag (final IDbItem item, final String tag, final MediaTagType type, final String mtc, final Date modified, final boolean deleted) throws DbException {
		try {
			return local_addTag(item, tag, type, mtc, false, modified, deleted);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void moveTags (final IDbItem from_item, final IDbItem to_item) throws DbException {
		try {
			local_moveTags(from_item, to_item);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeTag (final MediaTag tag) throws DbException {
		try {
			local_setTagDeleted(tag, tag, true, new Date());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void clearTags (final IDbItem item) throws DbException {
		try {
			local_clearTags(item);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void addTagClassification (final String classificationName) throws DbException {
		try {
			local_addTagClassification(classificationName);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public MediaTagClassification getTagClassification(final String classificationName) throws DbException {
		try {
			return local_getTagClassification(classificationName);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public methods for albums.

	@Override
	public Collection<MediaAlbum> getAlbums () throws DbException {
		try {
			return local_getAlbums();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public MediaAlbum createAlbum (final String name) throws DbException {
		try {
			return local_createAlbum(name);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public MediaAlbum getAlbum (final String name) throws DbException {
		try {
			return local_getAlbum(name);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeAlbum (final MediaAlbum album) throws DbException {
		try {
			local_removeAlbum(album);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void addToAlbum (final MediaAlbum album, final IDbItem item) throws DbException {
		try {
			local_addToAlbum(album, item);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeFromAlbum (final MediaAlbum album, final IDbItem item) throws DbException {
		try {
			local_removeFromAlbum(album, item);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public int removeFromAllAlbums (final IDbItem item) throws DbException {
		try {
			return local_removeFromAllAlbums(item);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public methods for Sources.

	@Override
	public List<String> getSources () throws DbException {
		try {
			return local_getSources();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void addSource (final String source) throws DbException {
		try {
			local_addSource(source);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeSource (final String source) throws DbException {
		try {
			local_removeSource(source);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Schema.

	/* - - - - - - - - - - - - - - - -
	 * tbl_prop
	 */

	private static final String SQL_TBL_PROP_EXISTS =
		"SELECT name FROM sqlite_master WHERE name='tbl_prop';";

	private static final String SQL_TBL_PROP_CREATE =
		"CREATE TABLE tbl_prop (" +
		"key VARCHAR(100) NOT NULL collate nocase primary key," +
		"value VARCHAR(1000)" +
		");";

	private static final String SQL_TBL_PROP_COL_KEY = "key";
	private static final String SQL_TBL_PROP_COL_VALUE = "value";

	/* - - - - - - - - - - - - - - - -
	 * tbl_tags
	 */

	private static final String SQL_TBL_TAGS_EXISTS =
		"SELECT name FROM sqlite_master WHERE name='tbl_tags';";

	private static final String SQL_TBL_TAGS_CREATE =
		"CREATE TABLE tbl_tags (" +
		"id INTEGER PRIMARY KEY AUTOINCREMENT," +
		"mf_id INT," +
		"tag VARCHAR(100)," +
		"type INT," +
		"cls_id INT," +
		"modified DATETIME," +
		"deleted INT(1)," +
		"FOREIGN KEY(mf_id) REFERENCES tbl_mediafiles(id) ON DELETE RESTRICT ON UPDATE RESTRICT," +
		"FOREIGN KEY(cls_id) REFERENCES tbl_tag_cls(id) ON DELETE RESTRICT ON UPDATE RESTRICT" +
		");";

	private static final String SQL_TBL_TAGS_COL_ROWID = "id";
//	private static final String SQL_TBL_TAGS_COL_MEDIAFILEROWID = "mf_id";
	private static final String SQL_TBL_TAGS_COL_TAG = "tag";
	private static final String SQL_TBL_TAGS_COL_TYPE = "type";
	private static final String SQL_TBL_TAGS_COL_CLSROWID = "cls_id";
	private static final String SQL_TBL_TAGS_COL_MODIFIED = "modified";
	private static final String SQL_TBL_TAGS_COL_DELETED = "deleted";

	/* - - - - - - - - - - - - - - - -
	 * tbl_tag_cls
	 */

	private static final String SQL_TBL_TAGCLS_EXISTS =
		"SELECT name FROM sqlite_master WHERE name='tbl_tag_cls';";

	private static final String SQL_TBL_TAGCLS_CREATE =
		"CREATE TABLE tbl_tag_cls (" +
		"id INTEGER PRIMARY KEY AUTOINCREMENT," +
		"cls VARCHAR(100) NOT NULL COLLATE NOCASE UNIQUE" +
		");";

	private static final String SQL_TBL_TAGCLS_COL_ROWID = "id";
	private static final String SQL_TBL_TAGCLS_COL_CLS = "cls";

	/* - - - - - - - - - - - - - - - -
	 * tbl_albums
	 */

	private static final String SQL_TBL_ALBUMS_EXISTS =
		"SELECT name FROM sqlite_master WHERE name='tbl_albums';";

	private static final String SQL_TBL_ALBUMS_CREATE =
		"CREATE TABLE tbl_albums (" +
		"id INTEGER PRIMARY KEY AUTOINCREMENT," +
		"name VARCHAR(255) NOT NULL COLLATE NOCASE UNIQUE" +
		");";

	private static final String SQL_TBL_ALBUMS_COL_ROWID = "id";
	private static final String SQL_TBL_ALBUMS_COL_NAME = "name";

	/* - - - - - - - - - - - - - - - -
	 * tbl_album_items
	 */

	protected static final String SQL_TBL_ALBUM_ITEMS = "tbl_album_items";

	private static final String SQL_TBL_ALBUM_ITEMS_EXISTS =
		"SELECT name FROM sqlite_master WHERE name='tbl_album_items';";

	private static final String SQL_TBL_ALBUM_ITEMS_CREATE =
		"CREATE TABLE tbl_album_items (" +
		"id INTEGER PRIMARY KEY AUTOINCREMENT," +
		"album_id INT," +
		"mf_id INT," +
		"FOREIGN KEY(album_id) REFERENCES tbl_albums(id) ON DELETE RESTRICT ON UPDATE RESTRICT," +
		"FOREIGN KEY(mf_id) REFERENCES tbl_mediafiles(id) ON DELETE RESTRICT ON UPDATE RESTRICT" +
		");";

//	private static final String SQL_TBL_ALBUM_ITEMS_COL_ROWID = "id";
//	private static final String SQL_TBL_ALBUM_ITEMS_COL_ALBUMROWID = "album_id";
//	private static final String SQL_TBL_ALBUM_ITEMS_COL_MEDIAFILEROWID = "mf_id";

	/* - - - - - - - - - - - - - - - -
	 * tbl_sources.
	 */

	private static final String SQL_TBL_SOURCES_EXISTS =
		"SELECT name FROM sqlite_master WHERE name='tbl_sources';";

	private static final String SQL_TBL_SOURCES_CREATE =
		"CREATE TABLE tbl_sources (" +
		"path VARCHAR(1000) NOT NULL collate nocase primary key" +
		");";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	SQL statements.

	/* - - - - - - - - - - - - - - - -
	 * tbl_prop.
	 */

	private static final String SQL_TBL_PROP_Q_GET =
		"SELECT value FROM tbl_prop WHERE key=?";

	private static final String SQL_TBL_PROP_Q_GET_ALL =
			"SELECT key,value FROM tbl_prop ORDER BY key, value";

	private static final String SQL_TBL_PROP_Q_INSERT =
		"INSERT INTO tbl_prop (key,value) VALUES (?,?)";
	private static final String SQL_TBL_PROP_Q_UPDATE =
		"UPDATE tbl_prop SET value=? WHERE key=?";
	private static final String SQL_TBL_PROP_Q_RM =
		"DELETE FROM tbl_prop WHERE key=?";

	/* - - - - - - - - - - - - - - - -
	 * tags.
	 */

	private static final String SQL_TBL_TAGS_Q_TOP =
		"SELECT count(*) as freq,t.id,t.tag,t.type,t.cls_id,t.modified,t.deleted,c.cls" +
		" FROM tbl_tags AS t LEFT OUTER JOIN tbl_tag_cls AS c ON t.cls_id=c.id" +
		" WHERE t.type=? AND (t.deleted IS NULL OR t.deleted!=1)" +
		" GROUP BY t.tag" +
		" ORDER BY freq DESC, t.type ASC, c.cls ASC, t.tag ASC;";

	private static final String SQL_TBL_TAGS_SEARCH =
		"SELECT count(*) as freq,t.id,t.tag,t.type,t.cls_id,t.modified,t.deleted,c.cls" +
		" FROM tbl_tags AS t LEFT OUTER JOIN tbl_tag_cls AS c ON t.cls_id=c.id" +
		" WHERE t.type=? AND (t.deleted IS NULL OR t.deleted!=1) AND tag LIKE ? ESCAPE ?" +
		" GROUP BY t.tag" +
		" ORDER BY freq DESC, t.type ASC, c.cls ASC, t.tag ASC;";

	private static final String SQL_TBL_TAGS_ADD =
		"INSERT INTO tbl_tags (mf_id,tag,type,cls_id,modified,deleted) VALUES (?,?,?,?,?,?);";

	private static final String SQL_TBL_TAGS_MOVE =
		"UPDATE tbl_tags SET mf_id=? WHERE mf_id=?;";

	private static final String SQL_TBL_TAGS_SET_DELETED =
		"UPDATE tbl_tags SET deleted=?,modified=? WHERE id=?;";

	private static final String SQL_TBL_TAGS_CLEAR =
		"DELETE FROM tbl_tags WHERE mf_id=?;";

	private static final String SQL_TBL_TAGS_Q_HASANY_INCLUDING_DELETED =
		"SELECT id FROM tbl_tags WHERE mf_id=?;";

	// TODO is there a nice way to merge these two?
	private static final String SQL_TBL_TAGS_Q_ALL =
		"SELECT t.id,t.tag,t.type,t.cls_id,t.modified,t.deleted,c.cls" +
		" FROM tbl_tags AS t LEFT OUTER JOIN tbl_tag_cls AS c ON t.cls_id=c.id" +
		" WHERE t.mf_id=? AND (t.deleted IS NULL OR t.deleted!=1)" +
		" ORDER BY t.type ASC, c.cls ASC, t.tag ASC;";
	private static final String SQL_TBL_TAGS_Q_ALL_INC_DELETED =
		"SELECT t.id,t.tag,t.type,t.cls_id,t.modified,t.deleted,c.cls" +
		" FROM tbl_tags AS t LEFT OUTER JOIN tbl_tag_cls AS c ON t.cls_id=c.id" +
		" WHERE t.mf_id=?" +
		" ORDER BY t.type ASC, c.cls ASC, t.tag ASC;";

	private static final String SQL_TBL_TAGS_Q_HASTAG =
		"SELECT t.id,t.tag,t.type,t.cls_id,t.modified,t.deleted,c.cls" +
		" FROM tbl_tags AS t LEFT OUTER JOIN tbl_tag_cls AS c ON t.cls_id=c.id" +
		" WHERE mf_id=? AND tag=? AND type=? AND cls_id=?;";

	private static final String SQL_TBL_TAGS_Q_HASTAG_CLSNULL =
		"SELECT t.id,t.tag,t.type,t.cls_id,t.modified,t.deleted,c.cls" +
		" FROM tbl_tags AS t LEFT OUTER JOIN tbl_tag_cls AS c ON t.cls_id=c.id" +
		" WHERE mf_id=? AND tag=? AND type=? AND cls_id IS NULL;";

	private static final String SQL_TBL_TAGCLS_ADD =
		"INSERT INTO tbl_tag_cls (cls) VALUES (?);";

	private static final String SQL_TBL_TAGCLS_Q_ALL =
		"SELECT id,cls FROM tbl_tag_cls;";

	private static final String SQL_TBL_TAGCLS_Q_CLS =
		"SELECT id,cls FROM tbl_tag_cls WHERE cls=?;";

	private static final String SQL_TBL_TAGCLS_Q_ROWID =
		"SELECT id,cls FROM tbl_tag_cls WHERE id=?;";

	/* - - - - - - - - - - - - - - - -
	 * albums.
	 */

	private static final String SQL_TBL_ALBUMS_Q_ALL =
		"SELECT a.id,a.name,count(i.mf_id) AS track_count" +
		" FROM tbl_albums AS a" +
		" LEFT OUTER JOIN tbl_album_items AS i ON a.id=i.album_id" +
		" LEFT OUTER JOIN tbl_mediafiles AS m ON i.mf_id=m.id AND m.type=" + MediaType.TRACK.getN() +
		" GROUP BY a.id" +
		" ORDER BY a.name ASC;";

	private static final String SQL_TBL_ALBUMS_Q_GET =
		"SELECT a.id,a.name,count(m.id) AS track_count" +
		" FROM tbl_albums AS a" +
		" LEFT OUTER JOIN tbl_album_items AS i ON a.id=i.album_id" +
		" LEFT OUTER JOIN (SELECT m.id,m.type FROM tbl_mediafiles AS m WHERE m.type=" + MediaType.TRACK.getN() + ") AS m ON i.mf_id=m.id" +
		" WHERE a.name=?" +
		" GROUP BY a.id;";

	private static final String SQL_TBL_ALBUM_ITEMS_Q_HAS =
		"SELECT id FROM tbl_album_items WHERE album_id=? AND mf_id=?;";

	private static final String SQL_TBL_ALBUMS_ADD =
		"INSERT INTO tbl_albums (name) VALUES (?);";

	private static final String SQL_TBL_ALBUMS_REMOVE =
		"DELETE FROM tbl_albums WHERE id=?;";

	private static final String SQL_TBL_ALBUM_ITEMS_ADD =
		"INSERT INTO tbl_album_items (album_id,mf_id) VALUES (?,?);";

	private static final String SQL_TBL_ALBUM_ITEMS_REMOVE =
		"DELETE FROM tbl_album_items WHERE album_id=? AND mf_id=?";

	private static final String SQL_TBL_ALBUM_ITEMS_REMOVE_FROM_ALL =
		"DELETE FROM tbl_album_items WHERE mf_id=?";

	/* - - - - - - - - - - - - - - - -
	 * tbl_sources.
	 */

	private static final String SQL_TBL_SOURCES_Q_ALL =
		"SELECT path FROM tbl_sources ORDER BY path ASC";

	private static final String SQL_TBL_SOURCES_ADD =
		"INSERT INTO tbl_sources (path) VALUES (?)";

	private static final String SQL_TBL_SOURCES_REMOVE =
		"DELETE FROM tbl_sources WHERE path=?";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Superclass methods.

	@Override
	protected List<SqlCreateCmd> getTblCreateCmds() {
		List<SqlCreateCmd> l = new ArrayList<>();

		l.add(new SqlCreateCmd(SQL_TBL_PROP_EXISTS, SQL_TBL_PROP_CREATE));

		l.add(new SqlCreateCmd(SQL_TBL_TAGCLS_EXISTS, SQL_TBL_TAGCLS_CREATE));
		l.add(new SqlCreateCmd("SELECT name FROM sqlite_master WHERE name='tag_cls_idx';", "CREATE UNIQUE INDEX tag_cls_idx ON tbl_tag_cls(id,cls);")); // TODO extract strings.

		l.add(new SqlCreateCmd(SQL_TBL_TAGS_EXISTS, SQL_TBL_TAGS_CREATE));
		l.add(new SqlCreateCmd("SELECT name FROM sqlite_master WHERE name='tags_idx';", "CREATE INDEX tags_idx ON tbl_tags(mf_id,tag);")); // TODO extract strings.

		l.add(new SqlCreateCmd(SQL_TBL_ALBUMS_EXISTS, SQL_TBL_ALBUMS_CREATE));
		l.add(new SqlCreateCmd(SQL_TBL_ALBUM_ITEMS_EXISTS, SQL_TBL_ALBUM_ITEMS_CREATE));
		// TODO any indexes required?

		l.add(new SqlCreateCmd(SQL_TBL_SOURCES_EXISTS, SQL_TBL_SOURCES_CREATE));

		return l;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Private methods for persisted props.

	private void local_setProp (final String key, final String value) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = null;
		try {
			if (value != null) {
				try {
					local_getProp(key);
					ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_UPDATE);
					ps.setString(1, value);
					ps.setString(2, key);
				}
				catch (final IllegalArgumentException e) {
					ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_INSERT);
					ps.setString(1, key);
					ps.setString(2, value);
				}
				if (ps.executeUpdate() < 1) throw new DbException("No update occured.");
			}
			else {
				ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_RM);
				ps.setString(1, key);
				ps.executeUpdate();
			}
			this.changeCaller.propertySet(key, value);
		}
		finally {
			if (ps != null) ps.close();
		}
	}

	private String local_getProp (final String key) throws SQLException, ClassNotFoundException {
		final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_GET);
		try {
			ps.setString(1, key);
			final ResultSet rs = ps.executeQuery();
			try {
				if (!rs.next()) { // True if there are rows in the result.
					throw new IllegalArgumentException("Did not find key '" + key + "'.");
				}
				return rs.getString(SQL_TBL_PROP_COL_VALUE);
			}
			finally {
				rs.close();
			}
		}
		finally {
			ps.close();
		}
	}

	private Map<String, String> local_getProps () throws SQLException, ClassNotFoundException {
		final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_GET_ALL);
		try {
			final ResultSet rs = ps.executeQuery();
			try {
				final Map<String, String> ret = new LinkedHashMap<>();
				while (rs.next()) {
					ret.put(rs.getString(SQL_TBL_PROP_COL_KEY), rs.getString(SQL_TBL_PROP_COL_VALUE));
				}
				return ret;
			}
			finally {
				rs.close();
			}
		}
		finally {
			ps.close();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Private methods for tags.

	private boolean local_addTag (final IDbItem item, final String tag, final MediaTagType type, final String clsName) throws SQLException, ClassNotFoundException, DbException {
		return local_addTag(item, tag, type, clsName, true, null, false);
	}

	private boolean local_addTag (final IDbItem item, final String tag, final MediaTagType type, final String clsName, final boolean dateNow, final Date modified, final boolean deleted) throws SQLException, ClassNotFoundException, DbException {
		MediaTagClassification mtc = null;
		if (clsName != null && !clsName.isEmpty()) {
			mtc = local_getTagClassification(clsName);
			if (mtc == null) mtc = local_addTagClassification(clsName);
		}
		return local_addTag(item, tag, type, mtc, dateNow, modified, deleted);
	}

	private boolean local_addTag (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc) throws SQLException, ClassNotFoundException, DbException {
		return local_addTag(item, tag, type, mtc, true, null, false);
	}

	private boolean local_addTag (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc, final boolean dateNow, final Date modified, final boolean deleted) throws SQLException, ClassNotFoundException, DbException {
		if (dateNow && modified != null) throw new IllegalArgumentException("if dateNow modified must be null.");
		if (dateNow && deleted) throw new IllegalArgumentException("if dateNow can not be deleted.");

		final List<MediaTag> existing = local_hasTag(item.getDbRowId(), tag, type, mtc);

		// Simple add.  modified MUST be null and deleted MUST be false.
		if (dateNow) {
			for (final MediaTag t : existing) {
				// If any not deleted, nothing to do.
				if (!t.isDeleted()) return false;
			}

			// Reinstate first item, if any.
			for (final MediaTag t : existing) {
				local_setTagDeleted(item, t, false, new Date());
				return true;
			}
		}
		else {
			for (final MediaTag t : existing) {
				if (!EqualHelper.areEqual(t.getModified(), modified) || !t.isDeleted() == deleted) {
					local_setTagDeleted(item, t, deleted, modified);
				}
				return true;
			}
		}

		final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_ADD);
		try {
			ps.setLong(1, item.getDbRowId());
			ps.setString(2, tag);
			ps.setInt(3, type.getIndex());

			if (mtc != null) {
				ps.setLong(4, mtc.getDbRowId());
			}
			else {
				ps.setNull(4, java.sql.Types.INTEGER);
			}

			if (dateNow) {
				ps.setDate(5, new java.sql.Date(System.currentTimeMillis()));
			}
			else if (modified != null) {
				ps.setDate(5, new java.sql.Date(modified.getTime()));
			}
			else {
				ps.setNull(5, java.sql.Types.DATE);
			}

			if (deleted) {
				ps.setInt(6, 1);
			}
			else {
				ps.setNull(6, java.sql.Types.INTEGER);
			}

			if (ps.executeUpdate() < 1) throw new DbException("No update occured.");
			this.changeCaller.mediaItemTagAdded(item, tag, type, mtc);
			return true;
		}
		finally {
			ps.close();
		}
	}

	private void local_moveTags (final IDbItem from_item, final IDbItem to_item) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_MOVE);
		try {
			ps.setLong(1, to_item.getDbRowId());
			ps.setLong(2, from_item.getDbRowId());

			int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for moveTags('" + from_item + "' to '" + to_item + "').");

			this.changeCaller.mediaItemTagsMoved(from_item, to_item);
		}
		finally {
			ps.close();
		}
	}

	private void local_setTagDeleted(final IDbItem item, final MediaTag tag, final boolean deleted, final Date modified) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_SET_DELETED);
		try {
			ps.setInt(1, deleted ? 1 : 0);

			if (modified != null) {
				ps.setDate(2, new java.sql.Date(modified.getTime()));
			}
			else {
				ps.setNull(2, java.sql.Types.DATE);
			}

			ps.setLong(3, tag.getDbRowId());

			int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured.");

			this.changeCaller.mediaItemTagAdded(item, tag.getTag(), tag.getType(), tag.getClassification());
		}
		finally {
			ps.close();
		}
	}

	private void local_clearTags(final IDbItem item) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_CLEAR);
		try {
			ps.setLong(1, item.getDbRowId());
			int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for clearTags('" + item + "').");

			this.changeCaller.mediaItemTagsCleared(item);
		}
		finally {
			ps.close();
		}
	}

	private boolean local_hasTagsIncludingDeleted (final long mf_rowId) throws SQLException, ClassNotFoundException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_Q_HASANY_INCLUDING_DELETED);
		try {
			ps.setLong(1, mf_rowId);
			ResultSet rs = ps.executeQuery();
			try {
				if (rs.next()) {
					return true;
				}
				return false;
			}
			finally {
				rs.close();
			}
		}
		finally {
			ps.close();
		}
	}

	private List<MediaTag> local_hasTag (final long mf_rowId, final String tag, final MediaTagType type, final MediaTagClassification mtc) throws SQLException, ClassNotFoundException, DbException {
		if (mtc != null) {
			return local_hasTag(mf_rowId, tag, type, mtc.getDbRowId());
		}
		return local_hasTag(mf_rowId, tag, type, 0);
	}

	private List<MediaTag> local_hasTag (final long mf_rowId, final String tag, final MediaTagType type, final long cls_rowid) throws SQLException, ClassNotFoundException, DbException {
		String sql;
		if (cls_rowid > 0 ) {
			sql = SQL_TBL_TAGS_Q_HASTAG;
		} else {
			sql = SQL_TBL_TAGS_Q_HASTAG_CLSNULL;
		}

		PreparedStatement ps = getDbCon().prepareStatement(sql);
		try {
			ps.setLong(1, mf_rowId);
			ps.setString(2, tag);
			ps.setInt(3, type.getIndex());
			if (cls_rowid > 0 ) {
				ps.setLong(4, cls_rowid);
			}
			ResultSet rs = ps.executeQuery();
			try {
				return local_readTags(rs);
			}
			finally {
				rs.close();
			}
		}
		finally {
			ps.close();
		}
	}

	private List<MediaTag> local_getTopTags (final int countLimit) throws SQLException, ClassNotFoundException, DbException {
		final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_Q_TOP);
		try {
			ps.setInt(1, MediaTagType.MANUAL.getIndex()); // Force this as including automatic tags makes no sense.
			ps.setMaxRows(countLimit);
			final ResultSet rs = ps.executeQuery();
			return local_readTags(rs);
		}
		finally {
			ps.close();
		}
	}

	private Map<String, MediaTag> local_tagSearch (final String query, MatchMode mode, final int resLimit) throws SQLException, ClassNotFoundException, DbException {
		String likeParam = SqliteHelper.escapeSearch(query) + "%";
		switch (mode) {
		case SUBSTRING:
			likeParam = "%" + likeParam;
			break;
		case PREFIX:
			break;
		default:
			throw new IllegalArgumentException("Unknown mode: " + mode);
		}

		final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_SEARCH);
		try {
			ps.setInt(1, MediaTagType.MANUAL.getIndex()); // Force this as including automatic tags makes no sense.
			ps.setString(2, likeParam);
			ps.setString(3, SqliteHelper.SEARCH_ESC);
			ps.setMaxRows(resLimit);
			final ResultSet rs = ps.executeQuery();
			try {
				Map<String, MediaTag> ret = null;
				while (rs.next()) {
					final MediaTag mt = local_readTag(rs);
					final int freq = rs.getInt("freq");
					if (ret == null) ret = new LinkedHashMap<>();
					ret.put(String.format("%s (%s)", mt.getTag(), freq), mt);
				}
				return ret != null ? ret : Collections.<String, MediaTag>emptyMap();
			}
			finally {
				rs.close();
			}
		}
		finally {
			ps.close();
		}
	}

	private List<MediaTag> local_getTags(final long mf_rowId, final boolean includeDeleted) throws SQLException, ClassNotFoundException, DbException {
		final PreparedStatement ps = getDbCon().prepareStatement(includeDeleted ? SQL_TBL_TAGS_Q_ALL_INC_DELETED : SQL_TBL_TAGS_Q_ALL);
		try {
			ps.setLong(1, mf_rowId);
			final ResultSet rs = ps.executeQuery();
			return local_readTags(rs);
		}
		finally {
			ps.close();
		}
	}

	private List<MediaTag> local_readTags (final ResultSet rs) throws SQLException, DbException, ClassNotFoundException {
		try {
			List<MediaTag> ret = null;
			while (rs.next()) {
				final MediaTag mt = local_readTag(rs);
				if (ret == null) ret = new ArrayList<>();
				ret.add(mt);
			}
			return ret != null ? ret : Collections.<MediaTag>emptyList();
		}
		finally {
			rs.close();
		}
	}

	private MediaTag local_readTag (final ResultSet rs) throws SQLException, DbException, ClassNotFoundException {
		final long rowId = rs.getLong(SQL_TBL_TAGS_COL_ROWID);
		final String tag = rs.getString(SQL_TBL_TAGS_COL_TAG);
		final int type = rs.getInt(SQL_TBL_TAGS_COL_TYPE);
		final long clsRowId = rs.getLong(SQL_TBL_TAGS_COL_CLSROWID);
		final Date modified = SqliteHelper.readDate(rs, SQL_TBL_TAGS_COL_MODIFIED);
		final boolean deleted = rs.getInt(SQL_TBL_TAGS_COL_DELETED) == 1; // default to false.

		final MediaTagType mtt = MediaTagType.getFromIndex(type);
		final MediaTagClassification mtc = local_getTagClassification(clsRowId);

		return new MediaTagImpl(rowId, tag, mtt, mtc, modified, deleted);
	}

	private MediaTagClassification local_addTagClassification (final String classificationName) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGCLS_ADD);
		try {
			ps.setString(1, classificationName);
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for addTagClassification('" + classificationName + "').");
			return local_getTagClassification(classificationName);
		} finally {
			ps.close();
		}
	}

	private List<MediaTagClassification> local_getTagClassifications () throws SQLException, ClassNotFoundException {
		List<MediaTagClassification> ret;
		Statement stat = getDbCon().createStatement();
		try {
			ResultSet rs = stat.executeQuery(SQL_TBL_TAGCLS_Q_ALL);
			try {
				ret = local_getTagClassification_parseRecordSet(rs);
			} finally {
				rs.close();
			}
		} finally {
			stat.close();
		}
		return ret;
	}

	private MediaTagClassification local_getTagClassification (final long clsRowId) throws DbException, SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		List<MediaTagClassification> ret;

		ps = getDbCon().prepareStatement(SQL_TBL_TAGCLS_Q_ROWID);
		try {
			ps.setLong(1, clsRowId);
			rs = ps.executeQuery();
			try {
				ret = local_getTagClassification_parseRecordSet(rs);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}

		if (ret.size() < 1) {
			return null;
		} else if (ret.size() == 1) {
			return ret.get(0);
		} else {
			throw new DbException("Query for TagClassification clsId='"+clsRowId+"' returned more than one result.");
		}
	}

	private MediaTagClassification local_getTagClassification (final String classificationName) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ResultSet rs;
		List<MediaTagClassification> ret;

		ps = getDbCon().prepareStatement(SQL_TBL_TAGCLS_Q_CLS);
		try {
			ps.setString(1, classificationName);
			rs = ps.executeQuery();
			try {
				ret = local_getTagClassification_parseRecordSet(rs);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}

		if (ret.size() < 1) {
			return null;
		} else if (ret.size() == 1) {
			return ret.get(0);
		} else {
			throw new DbException("Query for TagClassification classificationName='"+classificationName+"' returned more than one result.");
		}
	}

	private final MediaTagClassificationFactory tagClsFactory = new MediaTagClassificationFactory();

	private List<MediaTagClassification> local_getTagClassification_parseRecordSet (final ResultSet rs) throws SQLException {
		final List<MediaTagClassification> ret = new ArrayList<>();
		while (rs.next()) {
			final long rowId = rs.getLong(SQL_TBL_TAGCLS_COL_ROWID);
			final String clsName = rs.getString(SQL_TBL_TAGCLS_COL_CLS);
			ret.add(this.tagClsFactory.manufacture(rowId, clsName));
		}
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Private methods for albums.

	private Collection<MediaAlbum> local_getAlbums () throws SQLException, ClassNotFoundException {
		Statement stat = getDbCon().createStatement();
		try {
			ResultSet rs = stat.executeQuery(SQL_TBL_ALBUMS_Q_ALL);
			try {
				List<MediaAlbum> ret = new ArrayList<>();
				while (rs.next()) {
					long rowId = rs.getLong(SQL_TBL_ALBUMS_COL_ROWID);
					String name = rs.getString(SQL_TBL_ALBUMS_COL_NAME);
					int trackCount = rs.getInt("track_count");
					ret.add(new MediaAlbumImpl(rowId, name, trackCount));
				}
				return ret;
			}
			finally {
				rs.close();
			}
		}
		finally {
			stat.close();
		}
	}

	private MediaAlbum local_createAlbum (final String name) throws DbException, SQLException, ClassNotFoundException {
		MediaAlbum album = local_getAlbum(name);
		if (album != null) return album;
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUMS_ADD);
		try {
			ps.setString(1, name);
			int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for createAlbum('" + name + "').");
		}
		finally {
			ps.close();
		}
		album = local_getAlbum(name);
		if (album == null) throw new DbException("Failed to find album that was just created: '" + name + "'.");
		return album;
	}

	private void local_removeAlbum (final MediaAlbum album) throws DbException, SQLException, ClassNotFoundException {
		// TODO remove missing items.
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUMS_REMOVE);
		try {
			ps.setLong(1, album.getDbRowId());
			int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured.");
		}
		finally {
			ps.close();
		}
	}

	/**
	 * Get album, or null if not found.
	 */
	private MediaAlbum local_getAlbum (final String qName) throws SQLException, ClassNotFoundException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUMS_Q_GET);
		try {
			ps.setString(1, qName);
			ResultSet rs = ps.executeQuery();
			try {
				if (!rs.next()) {
					return null;
				}
				long rowId = rs.getLong(SQL_TBL_ALBUMS_COL_ROWID);
				String name = rs.getString(SQL_TBL_ALBUMS_COL_NAME);
				int trackCount = rs.getInt("track_count");
				return new MediaAlbumImpl(rowId, name, trackCount);
			}
			finally {
				rs.close();
			}
		}
		finally {
			ps.close();
		}
	}

	private boolean local_albumHasItem (final MediaAlbum album, final IDbItem item) throws SQLException, ClassNotFoundException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUM_ITEMS_Q_HAS);
		try {
			ps.setLong(1, album.getDbRowId());
			ps.setLong(2, item.getDbRowId());
			ResultSet rs = ps.executeQuery();
			try {
				if (rs.next()) {
					return true;
				}
				return false;
			}
			finally {
				rs.close();
			}
		}
		finally {
			ps.close();
		}
	}

	private void local_addToAlbum (final MediaAlbum album, final IDbItem item) throws SQLException, ClassNotFoundException, DbException {
		if (local_albumHasItem(album, item)) return;
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUM_ITEMS_ADD);
		try {
			ps.setLong(1, album.getDbRowId());
			ps.setLong(2, item.getDbRowId());
			int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for addToAlbum(" + album.getDbRowId() + "," + item.getDbRowId() + ").");
		}
		finally {
			ps.close();
		}
	}

	private void local_removeFromAlbum (final MediaAlbum album, final IDbItem item) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUM_ITEMS_REMOVE);
		try {
			ps.setLong(1, album.getDbRowId());
			ps.setLong(2, item.getDbRowId());
			int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for removeFromAlbum(" + album.getDbRowId() + "," + item.getDbRowId() + ").");
		}
		finally {
			ps.close();
		}
	}

	private int local_removeFromAllAlbums (final IDbItem item) throws SQLException, ClassNotFoundException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUM_ITEMS_REMOVE_FROM_ALL);
		try {
			ps.setLong(1, item.getDbRowId());
			return ps.executeUpdate();
		}
		finally {
			ps.close();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Private methods for sources.

	private List<String> local_getSources () throws SQLException, ClassNotFoundException {
		List<String> ret;
		Statement stat = getDbCon().createStatement();
		try {
			ResultSet rs = stat.executeQuery(SQL_TBL_SOURCES_Q_ALL);
			try {
				ret = new ArrayList<>();
				while (rs.next()) {
					ret.add(rs.getString("path"));
				}
			} finally {
				rs.close();
			}
		} finally {
			stat.close();
		}
		return ret;
	}

	private void local_addSource (final String source) throws SQLException, ClassNotFoundException, DbException {
		int n;
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_SOURCES_ADD);
		try {
			ps.setString(1, source);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for addSource('"+source+"').");
	}

	private void local_removeSource (final String source) throws SQLException, ClassNotFoundException, DbException {
		int n;
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_SOURCES_REMOVE);
		try {
			ps.setString(1, source);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for removeSource('"+source+"').");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
