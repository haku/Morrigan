package com.vaguehope.morrigan.model.media.internal.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayerChangeListener;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.internal.MediaAlbumImpl;
import com.vaguehope.morrigan.model.media.internal.MediaTagClassificationFactory;
import com.vaguehope.morrigan.model.media.internal.MediaTagImpl;
import com.vaguehope.sqlitewrapper.DbException;
import com.vaguehope.sqlitewrapper.GenericSqliteLayer;

public abstract class MediaSqliteLayer<T extends IMediaItem> extends GenericSqliteLayer implements IMediaItemStorageLayer<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.

	protected MediaSqliteLayer (String dbFilePath, boolean autoCommit) throws DbException {
		super(dbFilePath, autoCommit);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Collection<IMediaItemStorageLayerChangeListener<T>> changeListeners = Collections.synchronizedList(new ArrayList<IMediaItemStorageLayerChangeListener<T>>());
	private final IMediaItemStorageLayerChangeListener<T> changeCaller = new IMediaItemStorageLayerChangeListenerAdaptor<T>(this.changeListeners);

	@Override
	public void addChangeListener(IMediaItemStorageLayerChangeListener<T> listener) {
		// TODO rewrite this to use a map instead?
		if (!this.changeListeners.contains(listener)) this.changeListeners.add(listener);
	}

	@Override
	public void removeChangeListener(IMediaItemStorageLayerChangeListener<T> listener) {
		this.changeListeners.remove(listener);
	}


	@Override
	public IMediaItemStorageLayerChangeListener<T> getChangeEventCaller () {
		return this.changeCaller;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public methods for persisted props.

	@Override
	public void setProp (String key, String value) throws DbException {
		try {
			local_setProp(key, value);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public String getProp (String key) throws DbException {
		try {
			return local_getProp(key);
		} catch (IllegalArgumentException e) {
			return null;
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public methods for tags.

	@Override
	public boolean hasTags (IDbItem item) throws DbException {
		try {
			return local_hasTags(item.getDbRowId());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean hasTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws DbException {
		try {
			return local_hasTag(item.getDbRowId(), tag, type, mtc);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<MediaTag> getTags (IDbItem item) throws DbException {
		try {
			return local_getTags(item.getDbRowId());
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
	public boolean addTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws DbException {
		try {
			return local_addTag(item, tag, type, mtc);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean addTag (IDbItem item, String tag, MediaTagType type, String mtc) throws DbException {
		try {
			return local_addTag(item, tag, type, mtc);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void moveTags (IDbItem from_item, IDbItem to_item) throws DbException {
		try {
			local_moveTags(from_item, to_item);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeTag (MediaTag tag) throws DbException {
		try {
			local_removeTag(tag);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void clearTags (IDbItem item) throws DbException {
		try {
			local_clearTags(item);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void addTagClassification (String classificationName) throws DbException {
		try {
			local_addTagClassification(classificationName);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public MediaTagClassification getTagClassification(String classificationName) throws DbException {
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
	public MediaAlbum createAlbum (String name) throws DbException {
		try {
			return local_createAlbum(name);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public MediaAlbum getAlbum (String name) throws DbException {
		try {
			return local_getAlbum(name);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeAlbum (MediaAlbum album) throws DbException {
		try {
			local_removeAlbum(album);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void addToAlbum (MediaAlbum album, IDbItem item) throws DbException {
		try {
			local_addToAlbum(album, item);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeFromAlbum (MediaAlbum album, IDbItem item) throws DbException {
		try {
			local_removeFromAlbum(album, item);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeFromAllAlbums (IDbItem item) throws DbException {
		try {
			local_removeFromAllAlbums(item);
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
	public void addSource (String source) throws DbException {
		try {
			local_addSource(source);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeSource (String source) throws DbException {
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
		"FOREIGN KEY(mf_id) REFERENCES tbl_mediafiles(id) ON DELETE RESTRICT ON UPDATE RESTRICT," +
		"FOREIGN KEY(cls_id) REFERENCES tbl_tag_cls(id) ON DELETE RESTRICT ON UPDATE RESTRICT" +
		");";

	private static final String SQL_TBL_TAGS_COL_ROWID = "id";
//	private static final String SQL_TBL_TAGS_COL_MEDIAFILEROWID = "mf_id";
	private static final String SQL_TBL_TAGS_COL_TAG = "tag";
	private static final String SQL_TBL_TAGS_COL_TYPE = "type";
	private static final String SQL_TBL_TAGS_COL_CLSROWID = "cls_id";

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

	private static final String SQL_TBL_PROP_Q_INSERT =
		"INSERT INTO tbl_prop (key,value) VALUES (?,?)";

	private static final String SQL_TBL_PROP_Q_UPDATE =
		"UPDATE tbl_prop SET value=? WHERE key=?";

	/* - - - - - - - - - - - - - - - -
	 * tags.
	 */

	private static final String SQL_TBL_TAGS_ADD =
		"INSERT INTO tbl_tags (mf_id,tag,type,cls_id) VALUES (?,?,?,?);";

	private static final String SQL_TBL_TAGS_MOVE =
		"UPDATE tbl_tags SET mf_id=? WHERE mf_id=?;";

	private static final String SQL_TBL_TAGS_REMOVE =
		"DELETE FROM tbl_tags WHERE id=?;";

	private static final String SQL_TBL_TAGS_CLEAR =
		"DELETE FROM tbl_tags WHERE mf_id=?;";

	private static final String SQL_TBL_TAGS_Q_HASANY =
		"SELECT id FROM tbl_tags WHERE mf_id=?;";

	private static final String SQL_TBL_TAGS_Q_ALL =
		"SELECT t.id,t.tag,t.type,t.cls_id,c.cls" +
		" FROM tbl_tags AS t LEFT OUTER JOIN tbl_tag_cls AS c ON t.cls_id=c.id" +
		" WHERE t.mf_id=?" +
		" ORDER BY t.type ASC, c.cls ASC, t.tag ASC;";

	private static final String SQL_TBL_TAGS_Q_HASTAG =
		"SELECT id FROM tbl_tags WHERE mf_id=? AND tag=? AND type=? AND cls_id=?;";

	private static final String SQL_TBL_TAGS_Q_HASTAG_CLSNULL =
		"SELECT id FROM tbl_tags WHERE mf_id=? AND tag=? AND type=? AND cls_id IS NULL;";

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

	private static final String SQL_TBL_ALBUMS_Q_ALL = // TODO include album size.
		"SELECT a.id,a.name" +
		" FROM tbl_albums AS a" +
		" ORDER BY a.name ASC;";

	private static final String SQL_TBL_ALBUMS_Q_GET =
		"SELECT a.id,a.name FROM tbl_albums AS a WHERE name=?;";

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
		List<SqlCreateCmd> l = new ArrayList<SqlCreateCmd>();

		l.add(new SqlCreateCmd(SQL_TBL_PROP_EXISTS, SQL_TBL_PROP_CREATE));

		l.add(new SqlCreateCmd(SQL_TBL_TAGCLS_EXISTS, SQL_TBL_TAGCLS_CREATE));
		l.add(new SqlCreateCmd("SELECT name FROM sqlite_master WHERE name='tag_cls_idx';", "CREATE INDEX tag_cls_idx ON tbl_tag_cls(id,cls);")); // TODO extract strings.

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

	private void local_setProp (String key, String value) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = null;
		int n;

		try {
			try {
				local_getProp(key);
				ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_UPDATE);
				ps.setString(1, value);
				ps.setString(2, key);

			} catch (IllegalArgumentException e) {
				ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_INSERT);
				ps.setString(1, key);
				ps.setString(2, value);
			}

			n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");

			this.changeCaller.propertySet(key, value);
		}
		finally {
			if (ps!=null) ps.close();
		}
	}

	private String local_getProp (String key) throws SQLException, ClassNotFoundException {
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_GET);
			ps.setString(1, key);
			rs = ps.executeQuery();

			if (!rs.next()) { // True if there are rows in the result.
				throw new IllegalArgumentException("Did not find key '"+key+"'.");
			}

			String value = rs.getString(SQL_TBL_PROP_COL_VALUE);
			return value;

		} finally {
			if (rs != null) rs.close();
			if (ps != null) ps.close();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Private methods for tags.

	private boolean local_addTag (IDbItem item, String tag, MediaTagType type, String cls_name) throws SQLException, ClassNotFoundException, DbException {
		MediaTagClassification mtc = null;
		if (cls_name != null && !cls_name.isEmpty()) {
			mtc = local_getTagClassification(cls_name);
			if (mtc == null) mtc = local_addTagClassification(cls_name);
		}
		return local_addTag(item, tag, type, mtc);
	}

	private boolean local_addTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws SQLException, ClassNotFoundException, DbException {
		if (local_hasTag(item.getDbRowId(), tag, type, mtc)) {
			return false;
		}

		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_TAGS_ADD);
		int n;
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
			n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");

			this.changeCaller.mediaItemTagAdded(item, tag, type, mtc);

			return true;
		}
		finally {
			ps.close();
		}
	}

	private void local_moveTags (IDbItem from_item, IDbItem to_item) throws SQLException, ClassNotFoundException, DbException {
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

	private void local_removeTag(MediaTag tag) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_REMOVE);
		try {
			ps.setLong(1, tag.getDbRowId());
			int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured.");

			this.changeCaller.mediaItemTagRemoved(tag);
		}
		finally {
			ps.close();
		}
	}

	private void local_clearTags(IDbItem item) throws SQLException, ClassNotFoundException, DbException {
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

	private boolean local_hasTags (long mf_rowId) throws SQLException, ClassNotFoundException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_Q_HASANY);
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

	private boolean local_hasTag (long mf_rowId, String tag, MediaTagType type, MediaTagClassification mtc) throws SQLException, ClassNotFoundException {
		if (mtc != null) {
			return local_hasTag(mf_rowId, tag, type, mtc.getDbRowId());
		}
		return local_hasTag(mf_rowId, tag, type, 0);
	}

	private boolean local_hasTag (long mf_rowId, String tag, MediaTagType type, long cls_rowid) throws SQLException, ClassNotFoundException {
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

	private List<MediaTag> local_getTags(long mf_rowId) throws SQLException, ClassNotFoundException, DbException {
		List<MediaTag> ret = new ArrayList<MediaTag>();
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_Q_ALL);

		try {
			ps.setLong(1, mf_rowId);
			ResultSet rs = ps.executeQuery();
			try {
				while (rs.next()) {
					long rowId = rs.getLong(SQL_TBL_TAGS_COL_ROWID);
					String tag = rs.getString(SQL_TBL_TAGS_COL_TAG);
					int type = rs.getInt(SQL_TBL_TAGS_COL_TYPE);
					long clsRowId = rs.getLong(SQL_TBL_TAGS_COL_CLSROWID);

					MediaTagType mtt = MediaTagType.getFromIndex(type);
					MediaTagClassification mtc = local_getTagClassification(clsRowId);

					MediaTag mt = new MediaTagImpl(rowId, tag, mtt, mtc);
					ret.add(mt);
				}
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}

		return ret;
	}

	private MediaTagClassification local_addTagClassification (String classificationName) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGCLS_ADD);
		int n;
		try {
			ps.setString(1, classificationName);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for addTagClassification('"+classificationName+"').");

		MediaTagClassification ret = local_getTagClassification(classificationName);
		return ret;
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

	private MediaTagClassification local_getTagClassification (long clsRowId) throws DbException, SQLException, ClassNotFoundException {
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

	private MediaTagClassification local_getTagClassification (String classificationName) throws SQLException, ClassNotFoundException, DbException {
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

	private static List<MediaTagClassification> local_getTagClassification_parseRecordSet (ResultSet rs) throws SQLException {
		List<MediaTagClassification> ret = new ArrayList<MediaTagClassification>();

		while (rs.next()) {
			long rowId = rs.getLong(SQL_TBL_TAGCLS_COL_ROWID);
			String clsName = rs.getString(SQL_TBL_TAGCLS_COL_CLS);

			@SuppressWarnings("boxing")
			MediaTagClassification mtc = MediaTagClassificationFactory.INSTANCE.manufacture(rowId, clsName);
			ret.add(mtc);
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
				List<MediaAlbum> ret = new ArrayList<MediaAlbum>();
				while (rs.next()) {
					long rowId = rs.getLong(SQL_TBL_ALBUMS_COL_ROWID);
					String name = rs.getString(SQL_TBL_ALBUMS_COL_NAME);
					ret.add(new MediaAlbumImpl(rowId, name));
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

	private MediaAlbum local_createAlbum (String name) throws DbException, SQLException, ClassNotFoundException {
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

	private void local_removeAlbum (MediaAlbum album) throws DbException, SQLException, ClassNotFoundException {
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
	private MediaAlbum local_getAlbum (String qName) throws SQLException, ClassNotFoundException {
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
				return new MediaAlbumImpl(rowId, name);
			}
			finally {
				rs.close();
			}
		}
		finally {
			ps.close();
		}
	}

	private boolean local_albumHasItem (MediaAlbum album, IDbItem item) throws SQLException, ClassNotFoundException {
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

	private void local_addToAlbum (MediaAlbum album, IDbItem item) throws SQLException, ClassNotFoundException, DbException {
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

	private void local_removeFromAlbum (MediaAlbum album, IDbItem item) throws SQLException, ClassNotFoundException, DbException {
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

	private void local_removeFromAllAlbums (IDbItem item) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUM_ITEMS_REMOVE_FROM_ALL);
		try {
			ps.setLong(1, item.getDbRowId());
			int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for removeFromAllAlbums(" + item.getDbRowId() + ").");
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
				ret = new ArrayList<String>();
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

	private void local_addSource (String source) throws SQLException, ClassNotFoundException, DbException {
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

	private void local_removeSource (String source) throws SQLException, ClassNotFoundException, DbException {
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
