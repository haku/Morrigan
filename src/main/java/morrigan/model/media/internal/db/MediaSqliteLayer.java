package morrigan.model.media.internal.db;

import java.io.File;
import java.math.BigInteger;
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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import morrigan.model.db.IDbColumn;
import morrigan.model.db.IDbItem;
import morrigan.model.media.FileExistance;
import morrigan.model.media.MatchMode;
import morrigan.model.media.MediaAlbum;
import morrigan.model.media.MediaItem;
import morrigan.model.media.MediaStorageLayer;
import morrigan.model.media.MediaStorageLayerChangeListener;
import morrigan.model.media.MediaTag;
import morrigan.model.media.MediaTagClassification;
import morrigan.model.media.MediaTagType;
import morrigan.model.media.SortColumn;
import morrigan.model.media.MediaItem.MediaType;
import morrigan.model.media.SortColumn.SortDirection;
import morrigan.model.media.internal.MediaAlbumImpl;
import morrigan.model.media.internal.MediaTagClassificationFactory;
import morrigan.model.media.internal.MediaTagImpl;
import morrigan.sqlitewrapper.DbException;
import morrigan.sqlitewrapper.GenericSqliteLayer;
import morrigan.util.GeneratedString;

@SuppressWarnings("resource")
public class MediaSqliteLayer extends GenericSqliteLayer implements MediaStorageLayer {

	private static final Logger LOG = LoggerFactory.getLogger(MediaSqliteLayer.class);

	protected final DefaultMediaItemFactory itemFactory;

	public MediaSqliteLayer (final String dbFilePath, final boolean autoCommit, final DefaultMediaItemFactory itemFactory) throws DbException {
		super(dbFilePath, autoCommit);
		this.itemFactory = itemFactory;
	}

	private final Collection<MediaStorageLayerChangeListener> changeListeners = Collections.synchronizedList(new ArrayList<MediaStorageLayerChangeListener>());
	private final MediaStorageLayerChangeListener changeCaller = new MediaStorageLayerChangeListenerAdaptor(this.changeListeners);

	@Override
	public void addChangeListener(final MediaStorageLayerChangeListener listener) {
		// TODO rewrite this to use a map instead?
		if (!this.changeListeners.contains(listener)) this.changeListeners.add(listener);
	}

	@Override
	public void removeChangeListener(final MediaStorageLayerChangeListener listener) {
		this.changeListeners.remove(listener);
	}


	@Override
	public MediaStorageLayerChangeListener getChangeEventCaller () {
		return this.changeCaller;
	}

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

	/* - - - - - - - - - - - - - - - -
	 * tbl_media files.
	 */

	public static final String SQL_TBL_MEDIAFILES_NAME = "tbl_mediafiles";
	public static final IDbColumn[] SQL_TBL_MEDIAFILES_COLS = new IDbColumn[] {
		SQL_TBL_MEDIAFILES_COL_ID,
		SQL_TBL_MEDIAFILES_COL_FILE,
		SQL_TBL_MEDIAFILES_COL_TYPE,
		SQL_TBL_MEDIAFILES_COL_MIMETYPE,
		SQL_TBL_MEDIAFILES_COL_MD5,
		SQL_TBL_MEDIAFILES_COL_SHA1,
		SQL_TBL_MEDIAFILES_COL_DADDED,
		SQL_TBL_MEDIAFILES_COL_DMODIFIED,
		SQL_TBL_MEDIAFILES_COL_ENABLED,
		SQL_TBL_MEDIAFILES_COL_ENABLEDMODIFIED,
		SQL_TBL_MEDIAFILES_COL_MISSING,
		SQL_TBL_MEDIAFILES_COL_REMLOC,

		SQL_TBL_MEDIAFILES_COL_STARTCNT,
		SQL_TBL_MEDIAFILES_COL_ENDCNT,
		SQL_TBL_MEDIAFILES_COL_DLASTPLAY,
		SQL_TBL_MEDIAFILES_COL_DURATION,

		SQL_TBL_MEDIAFILES_COL_WIDTH,
		SQL_TBL_MEDIAFILES_COL_HEIGHT,
		};

	static protected List<IDbColumn> generateSqlTblMediaFilesColumns () {
		final List<IDbColumn> l = new ArrayList<>();
		for (final IDbColumn c : SQL_TBL_MEDIAFILES_COLS) {
			l.add(c);
		}
		return l;
	}

	@Override
	public List<IDbColumn> getMediaTblColumns () {
		return generateSqlTblMediaFilesColumns();
	}

	private static final String _SQL_MEDIAFILES_SELECT =
		"SELECT"
		+ " id, file, type, mimetype, md5, sha1, added, modified, enabled, enabledmodified, missing, remloc"
		+ ",startcnt,endcnt,lastplay,duration"
		+ ",width,height"
		+ " FROM tbl_mediafiles";

	private static final String _SQL_MEDIAFILESALBUMS_SELECT = // TODO FIXME is this the same as _SQL_MEDIAFILES_SELECT?
		"SELECT"
		+ " distinct m.id AS id,m.type AS type,mimetype,file,md5,sha1,added,modified,enabled,enabledmodified,missing,remloc,startcnt,endcnt,lastplay,duration,width,height"
		+ " FROM tbl_mediafiles AS m";

	private static final String _SQL_WHERE = " WHERE";
	private static final String _SQL_AND = " AND";

	private static final String _SQL_MEDIAFILESTAGS_WHERTYPE =
		" m.type=?";

	private static final String _SQL_MEDIAFILESALBUMS_WHEREALBUM =
		" m.id = tbl_album_items.mf_id AND tbl_album_items.album_id = ?";

	private static final String _SQL_MEDIAFILES_WHERENOTMISSING =
		" (missing<>1 OR missing is NULL)";

	private static final String _SQL_ORDERBYREPLACE =
		" ORDER BY {COL} {DIR};";

	private static final String _SQL_MEDIAFILES_WHEREFILEEQ =
		" file = ?";

	private static final String _SQL_MEDIAFILES_WHERE_MD5_EQ =
		" md5 = ?";

	private static final String SQL_TBL_MEDIAFILES_Q_MISSING =
		"SELECT missing FROM tbl_mediafiles WHERE file=?;";

	//	TODO move to helper / where DbColumn is defined?
	// WARNING: consuming code assumes the order of parameters in the generated SQL.
	private final GeneratedString sqlTblMediaFilesAdd = new GeneratedString() {
		@Override
		public String generateString() {
			final StringBuilder sb = new StringBuilder();
			final IDbColumn[] cols = SQL_TBL_MEDIAFILES_COLS;

			sb.append("INSERT INTO tbl_mediafiles (");
			boolean first = true;
			for (final IDbColumn c : cols) {
				if (c.getDefaultValue() != null) {
					if (first) {
						first = false;
					} else {
						sb.append(",");
					}
					sb.append(c.getName());
				}
			}
			sb.append(") VALUES (");
			first = true;
			for (final IDbColumn c : cols) {
				if (c.getDefaultValue() != null) {
					if (first) {
						first = false;
					} else {
						sb.append(",");
					}
					sb.append(c.getDefaultValue());
				}
			}
			sb.append(");");

			return sb.toString();
		}
	};

	private static final String SQL_TBL_MEDIAFILES_REMOVE =
		"DELETE FROM tbl_mediafiles WHERE file=?";

	private static final String SQL_TBL_MEDIAFILES_REMOVE_BYROWID =
		"DELETE FROM tbl_mediafiles WHERE id=?";

	private static final String SQL_TBL_MEDIAFILES_SETFILE =
		"UPDATE tbl_mediafiles SET file=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETDATEADDED =
		"UPDATE tbl_mediafiles SET added=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SET_MD5 =
		"UPDATE tbl_mediafiles SET md5=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SET_SHA1 =
		"UPDATE tbl_mediafiles SET sha1=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETDMODIFIED =
		"UPDATE tbl_mediafiles SET modified=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETENABLED =
		"UPDATE tbl_mediafiles SET enabled=?,enabledmodified=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETMISSING =
		"UPDATE tbl_mediafiles SET missing=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETREMLOC =
		"UPDATE tbl_mediafiles SET remloc=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETTYPE =
		"UPDATE tbl_mediafiles SET type=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETMIMETYPE =
			"UPDATE tbl_mediafiles SET mimetype=?" +
					" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_TRACKPLAYED =
		"UPDATE tbl_mediafiles SET startcnt=startcnt+?,lastplay=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_INCSTART =
		"UPDATE tbl_mediafiles SET startcnt=startcnt+?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETSTART =
		"UPDATE tbl_mediafiles SET startcnt=? WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_INCEND =
		"UPDATE tbl_mediafiles SET endcnt=endcnt+?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETEND =
		"UPDATE tbl_mediafiles SET endcnt=? WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETDATELASTPLAYED =
		"UPDATE tbl_mediafiles SET lastplay=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETDURATION =
		"UPDATE tbl_mediafiles SET duration=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETDIMENSIONS =
		"UPDATE tbl_mediafiles SET width=?,height=?" +
		" WHERE file=?;";

	@Override
	protected List<SqlCreateCmd> getTblCreateCmds() {
		final List<SqlCreateCmd> l = new ArrayList<>();

		l.add(new SqlCreateCmd(SQL_TBL_PROP_EXISTS, SQL_TBL_PROP_CREATE));

		l.add(new SqlCreateCmd(SQL_TBL_TAGCLS_EXISTS, SQL_TBL_TAGCLS_CREATE));
		l.add(new SqlCreateCmd("SELECT name FROM sqlite_master WHERE name='tag_cls_idx';", "CREATE UNIQUE INDEX tag_cls_idx ON tbl_tag_cls(id,cls);")); // TODO extract strings.

		l.add(new SqlCreateCmd(SQL_TBL_TAGS_EXISTS, SQL_TBL_TAGS_CREATE));
		l.add(new SqlCreateCmd("SELECT name FROM sqlite_master WHERE name='tags_idx';", "CREATE INDEX tags_idx ON tbl_tags(mf_id,tag);")); // TODO extract strings.

		l.add(new SqlCreateCmd(SQL_TBL_ALBUMS_EXISTS, SQL_TBL_ALBUMS_CREATE));
		l.add(new SqlCreateCmd(SQL_TBL_ALBUM_ITEMS_EXISTS, SQL_TBL_ALBUM_ITEMS_CREATE));
		// TODO any indexes required?

		l.add(new SqlCreateCmd(SQL_TBL_SOURCES_EXISTS, SQL_TBL_SOURCES_CREATE));

		// Insert at beginning as latter tables will have keys pointing to this one.
		l.add(0, SqliteHelper.generateSql_Create(SQL_TBL_MEDIAFILES_NAME, SQL_TBL_MEDIAFILES_COLS));

		// TODO Add indexes...
		//l.add(new SqlCreateCmd("SELECT name FROM sqlite_master WHERE name='foobar';",
		//"CREATE UNIQUE INDEX ..."));

		return l;
	}

	@Override
	protected void migrateDb() throws DbException {
		super.migrateDb();
		addColumnIfMissing(SQL_TBL_MEDIAFILES_COL_SHA1);
		addColumnIfMissing(SQL_TBL_MEDIAFILES_COL_MIMETYPE);
	}

	private void addColumnIfMissing(final IDbColumn column) throws DbException {
		try (final PreparedStatement p = getDbCon().prepareStatement("SELECT name FROM pragma_table_info('tbl_mediafiles') WHERE name=?;")) {
			p.setString(1, column.getName());
			try (final ResultSet rs = p.executeQuery()) {
				if (rs.next()) return;
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
		LOG.info("Adding column {} to tbl_mediafiles in: {}", column.getName(), getDbFilePath());
		final String sql = "ALTER TABLE tbl_mediafiles ADD COLUMN " + column.getName() + " " + column.getSqlType();
		try (final PreparedStatement p = getDbCon().prepareStatement(sql)) {
			p.execute();
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setProp (final String key, final String value) throws DbException {
		try {
			if (value != null) {
				final String prev = getProp(key);
				if (prev != null) {
					try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_UPDATE)) {
						ps.setString(1, value);
						ps.setString(2, key);
						if (ps.executeUpdate() < 1) throw new DbException("No update occured.");
					}
				}
				else {
					try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_INSERT)) {
						ps.setString(1, key);
						ps.setString(2, value);
						if (ps.executeUpdate() < 1) throw new DbException("No update occured.");
					}
				}
			}
			else {
				try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_RM)) {
					ps.setString(1, key);
					ps.executeUpdate();
					if (ps.executeUpdate() < 1) throw new DbException("No update occured.");
				}
			}
			this.changeCaller.propertySet(key, value);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public String getProp (final String key) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_GET)) {
			ps.setString(1, key);
			try (final ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) { // True if there are rows in the result.
					return null;
				}
				return rs.getString(SQL_TBL_PROP_COL_VALUE);
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public Map<String, String> getProps () throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_PROP_Q_GET_ALL)) {
			try (final ResultSet rs = ps.executeQuery()) {
				final Map<String, String> ret = new LinkedHashMap<>();
				while (rs.next()) {
					ret.put(rs.getString(SQL_TBL_PROP_COL_KEY), rs.getString(SQL_TBL_PROP_COL_VALUE));
				}
				return ret;
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean addTag (final IDbItem item, final String tag, final MediaTagType type, final String clsName) throws DbException {
		return _addTag(item, tag, type, clsName, true, null, false);
	}

	@Override
	public boolean addTag (final IDbItem item, final String tag, final MediaTagType type, final String mtc, final Date modified, final boolean deleted) throws DbException {
		return _addTag(item, tag, type, mtc, false, modified, deleted);
	}

	private boolean _addTag (final IDbItem item, final String tag, final MediaTagType type, final String clsName, final boolean dateNow, final Date modified, final boolean deleted) throws DbException {
		MediaTagClassification mtc = null;
		if (clsName != null && !clsName.isEmpty()) {
			mtc = getTagClassification(clsName);
			if (mtc == null) mtc = addTagClassification(clsName);
		}
		return _addTag(item, tag, type, mtc, dateNow, modified, deleted);
	}

	@Override
	public boolean addTag (final IDbItem item, final String tag) throws DbException {
		return _addTag(item, tag, MediaTagType.MANUAL, (MediaTagClassification) null, true, null, false);
	}

	private boolean _addTag (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc, final boolean dateNow, final Date modified, final boolean deleted) throws DbException {
		if (dateNow && modified != null) throw new IllegalArgumentException("if dateNow modified must be null.");
		if (dateNow && deleted) throw new IllegalArgumentException("if dateNow can not be deleted.");

		final List<MediaTag> existing = _hasTag(item.getDbRowId(), tag, type, mtc);

		// Simple add.  modified MUST be null and deleted MUST be false.
		if (dateNow) {
			for (final MediaTag t : existing) {
				// If any not deleted, nothing to do.
				if (!t.isDeleted()) return false;
			}

			// Reinstate first item, if any.
			for (final MediaTag t : existing) {
				_setTagDeleted(item, t, false, new Date());
				return true;
			}
		}
		else {
			for (final MediaTag t : existing) {
				if (!Objects.equals(t.getModified(), modified) || !t.isDeleted() == deleted) {
					_setTagDeleted(item, t, deleted, modified);
				}
				return true;
			}
		}

		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_ADD)) {
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
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void moveTags (final IDbItem from_item, final IDbItem to_item) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_MOVE)) {
			ps.setLong(1, to_item.getDbRowId());
			ps.setLong(2, from_item.getDbRowId());

			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for moveTags('" + from_item + "' to '" + to_item + "').");

			this.changeCaller.mediaItemTagsMoved(from_item, to_item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeTag (final MediaTag tag) throws DbException {
		_setTagDeleted(tag, tag, true, new Date());
	}

	private void _setTagDeleted(final IDbItem item, final MediaTag tag, final boolean deleted, final Date modified) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_SET_DELETED)) {
			ps.setInt(1, deleted ? 1 : 0);

			if (modified != null) {
				ps.setDate(2, new java.sql.Date(modified.getTime()));
			}
			else {
				ps.setNull(2, java.sql.Types.DATE);
			}

			ps.setLong(3, tag.getDbRowId());

			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured.");

			this.changeCaller.mediaItemTagAdded(item, tag.getTag(), tag.getType(), tag.getClassification());
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void clearTags (final IDbItem item) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_CLEAR)) {
			ps.setLong(1, item.getDbRowId());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for clearTags('" + item + "').");

			this.changeCaller.mediaItemTagsCleared(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean hasTagsIncludingDeleted (final IDbItem item) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_Q_HASANY_INCLUDING_DELETED)) {
			ps.setLong(1, item.getDbRowId());
			try (final ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean hasTag (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc) throws DbException {
		for (final MediaTag t : _hasTag(item.getDbRowId(), tag, type, mtc)) {
			if (!t.isDeleted()) return true;
		}
		return false;
	}

	private List<MediaTag> _hasTag (final long mf_rowId, final String tag, final MediaTagType type, final MediaTagClassification mtc) throws DbException {
		if (mtc != null) {
			return _hasTag(mf_rowId, tag, type, mtc.getDbRowId());
		}
		return _hasTag(mf_rowId, tag, type, 0);
	}

	private List<MediaTag> _hasTag (final long mf_rowId, final String tag, final MediaTagType type, final long cls_rowid) throws DbException {
		String sql;
		if (cls_rowid > 0 ) {
			sql = SQL_TBL_TAGS_Q_HASTAG;
		} else {
			sql = SQL_TBL_TAGS_Q_HASTAG_CLSNULL;
		}

		try (final PreparedStatement ps = getDbCon().prepareStatement(sql)) {
			ps.setLong(1, mf_rowId);
			ps.setString(2, tag);
			ps.setInt(3, type.getIndex());
			if (cls_rowid > 0 ) {
				ps.setLong(4, cls_rowid);
			}
			try (final ResultSet rs = ps.executeQuery()) {
				return _readTags(rs);
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<MediaTag> getTopTags (final int countLimit) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_Q_TOP)) {
			ps.setInt(1, MediaTagType.MANUAL.getIndex()); // Force this as including automatic tags makes no sense.
			ps.setMaxRows(countLimit);
			final ResultSet rs = ps.executeQuery();
			return _readTags(rs);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public Map<String, MediaTag> tagSearch (final String query, final MatchMode mode, final int resLimit) throws DbException {
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

		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_SEARCH)) {
			ps.setInt(1, MediaTagType.MANUAL.getIndex()); // Force this as including automatic tags makes no sense.
			ps.setString(2, likeParam);
			ps.setString(3, SqliteHelper.SEARCH_ESC);
			ps.setMaxRows(resLimit);
			try (final ResultSet rs = ps.executeQuery()) {
				Map<String, MediaTag> ret = null;
				while (rs.next()) {
					final MediaTag mt = _readTag(rs);
					final int freq = rs.getInt("freq");
					if (ret == null) ret = new LinkedHashMap<>();
					ret.put(String.format("%s (%s)", mt.getTag(), freq), mt);
				}
				return ret != null ? ret : Collections.<String, MediaTag>emptyMap();
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<MediaTag> getTags (final IDbItem item, final boolean includeDeleted) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(includeDeleted ? SQL_TBL_TAGS_Q_ALL_INC_DELETED : SQL_TBL_TAGS_Q_ALL)) {
			ps.setLong(1, item.getDbRowId());
			final ResultSet rs = ps.executeQuery();
			return _readTags(rs);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	private List<MediaTag> _readTags (final ResultSet rs) throws DbException, SQLException {
		try {
			List<MediaTag> ret = null;
			while (rs.next()) {
				final MediaTag mt = _readTag(rs);
				if (ret == null) ret = new ArrayList<>();
				ret.add(mt);
			}
			return ret != null ? ret : Collections.<MediaTag>emptyList();
		}
		finally {
			rs.close();
		}
	}

	private MediaTag _readTag (final ResultSet rs) throws DbException, SQLException {
		final long rowId = rs.getLong(SQL_TBL_TAGS_COL_ROWID);
		final String tag = rs.getString(SQL_TBL_TAGS_COL_TAG);
		final int type = rs.getInt(SQL_TBL_TAGS_COL_TYPE);
		final long clsRowId = rs.getLong(SQL_TBL_TAGS_COL_CLSROWID);
		final Date modified = SqliteHelper.readDate(rs, SQL_TBL_TAGS_COL_MODIFIED);
		final boolean deleted = rs.getInt(SQL_TBL_TAGS_COL_DELETED) == 1; // default to false.

		final MediaTagType mtt = MediaTagType.getFromIndex(type);
		final MediaTagClassification mtc = _getTagClassification(clsRowId);

		return new MediaTagImpl(rowId, tag, mtt, mtc, modified, deleted);
	}

	@Override
	public MediaTagClassification addTagClassification (final String classificationName) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGCLS_ADD)) {
			ps.setString(1, classificationName);
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for addTagClassification('" + classificationName + "').");
			return getTagClassification(classificationName);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	private MediaTagClassification _getTagClassification (final long clsRowId) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGCLS_Q_ROWID)) {
			ps.setLong(1, clsRowId);
			try (ResultSet rs = ps.executeQuery()) {
				final List<MediaTagClassification> ret = _getTagClassification_parseRecordSet(rs);
				if (ret.size() < 1) {
					return null;
				} else if (ret.size() == 1) {
					return ret.get(0);
				} else {
					throw new DbException("Query for TagClassification clsId='"+clsRowId+"' returned more than one result.");
				}
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}

	}

	@Override
	public MediaTagClassification getTagClassification(final String classificationName) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGCLS_Q_CLS)) {
			ps.setString(1, classificationName);
			try (ResultSet rs = ps.executeQuery()) {
				final List<MediaTagClassification> ret = _getTagClassification_parseRecordSet(rs);
				if (ret.size() < 1) {
					return null;
				} else if (ret.size() == 1) {
					return ret.get(0);
				} else {
					throw new DbException("Query for TagClassification classificationName='"+classificationName+"' returned more than one result.");
				}
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	private final MediaTagClassificationFactory tagClsFactory = new MediaTagClassificationFactory();

	private List<MediaTagClassification> _getTagClassification_parseRecordSet (final ResultSet rs) throws SQLException {
		final List<MediaTagClassification> ret = new ArrayList<>();
		while (rs.next()) {
			final long rowId = rs.getLong(SQL_TBL_TAGCLS_COL_ROWID);
			final String clsName = rs.getString(SQL_TBL_TAGCLS_COL_CLS);
			ret.add(this.tagClsFactory.manufacture(rowId, clsName));
		}
		return ret;
	}

	@Override
	public List<MediaItem> getAllMedia (final SortColumn[] sorts, final SortDirection[] directions, final boolean hideMissing) throws DbException {
		return getMedia(MediaType.UNKNOWN, sorts, directions, hideMissing);
	}

	@Override
	public List<MediaItem> getMedia (final MediaType mediaType, final SortColumn[] sorts, final SortDirection[] directions, final boolean hideMissing) throws DbException {
		try {
			return SearchParser.parseSearch(mediaType, sorts, directions, hideMissing, false).execute(getDbCon(), this.itemFactory);
		}
		catch (final Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<MediaItem> getMedia (final MediaType mediaType, final SortColumn[] sorts, final SortDirection[] directions, final boolean hideMissing, final String search) throws DbException {
		try {
			return SearchParser.parseSearch(mediaType, sorts, directions, hideMissing, false, search).execute(getDbCon(), this.itemFactory);
		}
		catch (final Exception e) {
			throw new DbException(e);
		}
	}

	/**
	 * Querying for type UNKNOWN will return all types (i.e. wild-card).
	 */
	@Override
	public List<MediaItem> search(final MediaType mediaType, final String term, final int maxResults, final SortColumn[] sortColumn, final SortDirection[] sortDirection, final boolean includeDisabled) throws DbException {
		try {
			return SearchParser.parseSearch(mediaType, sortColumn, sortDirection, true, !includeDisabled, term).execute(getDbCon(), this.itemFactory, maxResults);
		}
		catch (final Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public Collection<MediaItem> getAlbumItems (final MediaType mediaType, final MediaAlbum album) throws DbException {
		List<MediaItem> ret;

		final StringBuilder sql = new StringBuilder();
		sql.append(_SQL_MEDIAFILESALBUMS_SELECT);
		sql.append(",").append(SQL_TBL_ALBUM_ITEMS);
		sql.append(_SQL_WHERE);
		if (mediaType != MediaType.UNKNOWN) {
			sql.append(_SQL_MEDIAFILESTAGS_WHERTYPE); // type param.
			sql.append(_SQL_AND);
		}
		sql.append(_SQL_MEDIAFILESALBUMS_WHEREALBUM); // album_id param.
		sql.append(_SQL_AND);
		sql.append(_SQL_MEDIAFILES_WHERENOTMISSING);
		sql.append(_SQL_ORDERBYREPLACE.replace("{COL}", SQL_TBL_MEDIAFILES_COL_FILE.getName()).replace("{DIR}", "ASC"));

		try (final PreparedStatement ps = getDbCon().prepareStatement(sql.toString())) {
			if (mediaType != MediaType.UNKNOWN) {
				ps.setInt(1, mediaType.getN());
				ps.setLong(2, album.getDbRowId());
			}
			else {
				ps.setLong(1, album.getDbRowId());
			}
			final ResultSet rs = ps.executeQuery();
			try {
				ret = local_parseRecordSet(rs, this.itemFactory);
			}
			finally {
				rs.close();
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
		return ret;
	}

	@Override
	public FileExistance hasFile (final File file) throws DbException {
		return hasFile(file.getAbsolutePath());
	}

	@Override
	public FileExistance hasFile (final String filePath) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_Q_MISSING)) {
			ps.setString(1, filePath);
			final ResultSet rs = ps.executeQuery();
			try {
				if (rs.next()) {
					final boolean missing = rs.getInt(1) == 1; // default to false.
					if (rs.next()) throw new DbException(String.format("Path %s in DB more than once.", filePath));
					return missing ? FileExistance.MISSING : FileExistance.EXISTS;
				}
				return FileExistance.UNKNOWN;
			}
			finally {
				rs.close();
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public MediaItem getByFile (final File file) throws DbException {
		return getByFile(file.getAbsolutePath());
	}

	@Override
	public MediaItem getByFile (final String filePath) throws DbException {
		final String sql = _SQL_MEDIAFILES_SELECT + _SQL_WHERE + _SQL_MEDIAFILES_WHEREFILEEQ;
		try (final PreparedStatement ps = getDbCon().prepareStatement(sql)) {
			ps.setString(1, filePath);
			ps.setMaxRows(2); // Ask for 1, so we know if there is more than 1.

			final List<MediaItem> res;
			try (final ResultSet rs = ps.executeQuery()) {
				res = local_parseRecordSet(rs, this.itemFactory);
			}
			if (res.size() == 1) return res.get(0);
			throw new IllegalArgumentException("File not found '" + filePath + "' (results count = " + res.size() + ").");
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public MediaItem getByMd5 (final BigInteger md5) throws DbException {
		final String sql = _SQL_MEDIAFILES_SELECT + _SQL_WHERE + _SQL_MEDIAFILES_WHERE_MD5_EQ;
		try (final PreparedStatement ps = getDbCon().prepareStatement(sql)) {
			ps.setBytes(1, md5.toByteArray());
			ps.setMaxRows(2); // Ask for 2 so we know if there is more than 1.
			try (final ResultSet rs = ps.executeQuery()) {
				final List<MediaItem> res = local_parseRecordSet(rs, this.itemFactory);
				if (res.size() == 1) return res.get(0);
				if (res.size() == 0) return null;
				throw new IllegalArgumentException("Ambigious MD5 '" + md5.toString(16) + "': results count = " + res.size());
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean addFile (final MediaType mediaType, final File file) throws DbException {
		return addFile(mediaType, file.getAbsolutePath(), file.lastModified());
	}

	@Override
	public boolean addFile (final MediaType mediaType, final String filePath, final long lastModified) throws DbException {
		if (!hasFile(filePath).isKnown()) {
			try (final PreparedStatement ps = getDbCon().prepareStatement(this.sqlTblMediaFilesAdd.toString())) {
				// WARNING: this assumes the order of parameters in the above SQL.
				ps.setString(1, filePath);
				ps.setInt(2, mediaType.getN());
				ps.setDate(3, new java.sql.Date(new Date().getTime()));
				ps.setDate(4, new java.sql.Date(lastModified));
				final int n = ps.executeUpdate();
				if (n < 1) throw new DbException("No update occured for addTrack('" + filePath + "','" + lastModified + "').");
			}
			catch (final SQLException e) {
				throw new DbException(e);
			}
			getChangeEventCaller().mediaItemAdded(filePath);
			return true;
		}
		return false;
	}

	@Override
	public boolean[] addFiles (final MediaType mediaType,final List<File> filesToAdd) throws DbException {
		final boolean[] ret = new boolean[filesToAdd.size()];
		final List<File> unknownFiles = new ArrayList<>(filesToAdd.size());

		for (int i = 0; i < filesToAdd.size(); i++) {
			final File file = filesToAdd.get(i);

			if (file == null) throw new IllegalArgumentException("File can not be null.");

			final String filePath = file.getAbsolutePath();
			if (filePath == null || filePath.isEmpty()) throw new IllegalArgumentException("filePath is null or empty: " + filePath);

			switch (hasFile(filePath)) {
				case UNKNOWN:
					unknownFiles.add(file);
					break;
				case MISSING:
					// A long long time ago, I thought it would be a good idea to make the file column NOCASE.
					// This means if a path changes case, the new file can not be inserted along side the old one.
					// Hopefully it will not happen to often, so not worried about performance.
					local_renameFile(filePath, filePath);
					ret[i] = true;
					unknownFiles.add(null); // Placeholder.
					break;
				default:
					ret[i] = false;
					unknownFiles.add(null); // Placeholder.
					break;
			}
		}

		if (filesToAdd.size() != unknownFiles.size()) throw new IllegalStateException("filesToAdd.size() != unknownFiles.size()");

		try (final PreparedStatement ps = getDbCon().prepareStatement(this.sqlTblMediaFilesAdd.toString())) {
			for (final File file : unknownFiles) {
				if (file == null) continue; // Ignore placeholders.
				// WARNING: this assumes the order of parameters in the above SQL.
				ps.setString(1, file.getAbsolutePath());
				ps.setInt(2, mediaType.getN());
				ps.setDate(3, new java.sql.Date(new Date().getTime()));
				ps.setDate(4, new java.sql.Date(file.lastModified()));
				ps.addBatch();
			}

			final int[] batchRes = ps.executeBatch();

			final List<File> addedFiles = new ArrayList<>();
			int ri = 0;

			for (int fi = 0; fi < unknownFiles.size(); fi++) {
				final File file = unknownFiles.get(fi);
				if (file == null) continue;

				final int result = batchRes[ri];
				ri++;

				final boolean successfullyAdded = (result > 0 || result == Statement.SUCCESS_NO_INFO);
				if (successfullyAdded) addedFiles.add(file);
				ret[fi] = successfullyAdded;
			}

			getChangeEventCaller().mediaItemsAdded(addedFiles);

			return ret;
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	protected void local_renameFile (final String oldPath, final String newPath) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETFILE)) {
			ps.setString(1, newPath);
			ps.setString(2, oldPath);
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_renameFile('" + oldPath + "','" + newPath + "').");
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public int removeFile (final String sfile) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_REMOVE)) {
			ps.setString(1, sfile);
			final int ret = ps.executeUpdate();
			if (ret > 0) getChangeEventCaller().mediaItemRemoved(sfile);
			return ret;
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public int removeFile (final IDbItem dbItem) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_REMOVE_BYROWID)) {
			ps.setLong(1, dbItem.getDbRowId());
			final int ret = ps.executeUpdate();
			if (ret > 0) getChangeEventCaller().mediaItemRemoved(null); // FIXME pass a useful parameter here.
			return ret;
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}

	}

	@Override
	public void setDateAdded (final MediaItem item, final Date date) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDATEADDED)) {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setDateAdded('" + item + "','" + date + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setMd5 (final MediaItem item, final BigInteger md5) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SET_MD5)) {
			if (md5 != null) {
				ps.setBytes(1, md5.toByteArray());
			}
			else {
				ps.setNull(1, java.sql.Types.BLOB);
			}
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setMd5('" + item + "','" + md5 + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setSha1(final MediaItem item, final BigInteger sha1) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SET_SHA1)) {
			if (sha1 != null) {
				ps.setBytes(1, sha1.toByteArray());
			}
			else {
				ps.setNull(1, java.sql.Types.BLOB);
			}
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setSha1('" + item + "','" + sha1 + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setDateLastModified (final MediaItem item, final Date date) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDMODIFIED)) {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setDateLastModified('" + item + "','" + date + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setEnabled (final MediaItem item, final boolean value) throws DbException {
		_setEnabled(item, value, true, null);
	}

	@Override
	public void setEnabled (final MediaItem item, final boolean value, final Date lastModified) throws DbException {
		_setEnabled(item, value, false, lastModified);
	}

	protected void _setEnabled (final MediaItem item, final boolean value, final boolean nullIsNow, final Date lastModified) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETENABLED)) {
			ps.setInt(1, value ? 1 : 0);

			if (lastModified != null) {
				ps.setDate(2, new java.sql.Date(lastModified.getTime()));
			}
			else if (nullIsNow) {
				ps.setDate(2, new java.sql.Date(System.currentTimeMillis()));
			}
			else {
				ps.setNull(2, java.sql.Types.DATE);
			}

			ps.setString(3, item.getFilepath());

			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setEnabled('" + item + "','" + value + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setMissing (final MediaItem item, final boolean value) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETMISSING)) {
			ps.setInt(1, value ? 1 : 0);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setMissing('" + item + "','" + value + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setRemoteLocation (final MediaItem item, final String remoteLocation) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETREMLOC)) {
			ps.setString(1, remoteLocation);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured for local_setRemoteLocation('"+item+"','"+remoteLocation+"').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setItemMediaType (final MediaItem item, final MediaType newType) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETTYPE)) {
			ps.setInt(1, newType.getN());
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setItemMimeType (final MediaItem item, final String newType) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETMIMETYPE)) {
			ps.setString(1, newType);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackPlayed (final MediaItem item) throws DbException {
		_trackPlayed(item, 1, new Date());
	}

	protected void _trackPlayed (final MediaItem item, final long x, final Date date) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_TRACKPLAYED)) {
			ps.setLong(1, x);
			ps.setDate(2, new java.sql.Date(date.getTime()));
			ps.setString(3, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackStartCnt (final MediaItem item, final long x) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCSTART)) {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackStartCnt (final MediaItem item, final long x) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETSTART)) {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setDateLastPlayed (final MediaItem item, final Date date) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDATELASTPLAYED)) {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackFinished (final MediaItem item) throws DbException {
		_incTrackEndCnt(item, 1);
	}

	@Override
	public void incTrackEndCnt (final MediaItem item, final long n) throws DbException {
		_incTrackEndCnt(item, n);
	}

	protected void _incTrackEndCnt (final MediaItem item, final long x) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCEND)) {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackEndCnt (final MediaItem item, final long x) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETEND);) {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackDuration (final MediaItem item, final int duration) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDURATION)) {
			ps.setInt(1, duration);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setDimensions (final MediaItem item, final int width, final int height) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDIMENSIONS)) {
			ps.setInt(1, width);
			ps.setInt(2, height);
			ps.setString(3, item.getFilepath());
			int n;
			n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for setRemoteLocation('" + item + "','" + width + "','" + height + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public MediaItem getNewT (final String filePath) {
		return this.itemFactory.getNewMediaItem(filePath);
	}

	private static class ColumnIndexes {
		final int colFile;
		final int colType;
		final int colMimeType;
		final int colAdded;
		final int colMd5;
		final int colSha1;
		final int colModified;
		final int colEnabled;
		final int colEnabledModified;
		final int colMissing;
		final int colId;
		final int colRemLoc;
		final int colStartCount;
		final int colEndCount;
		final int colDuration;
		final int colLastPlay;
		final int colWidth;
		final int colHeight;

		public ColumnIndexes (final ResultSet rs) throws SQLException {
			this.colFile = rs.findColumn(SQL_TBL_MEDIAFILES_COL_FILE.getName());
			this.colType = rs.findColumn(SQL_TBL_MEDIAFILES_COL_TYPE.getName());
			this.colMimeType = rs.findColumn(SQL_TBL_MEDIAFILES_COL_MIMETYPE.getName());
			this.colAdded = rs.findColumn(SQL_TBL_MEDIAFILES_COL_DADDED.getName());
			this.colMd5 = rs.findColumn(SQL_TBL_MEDIAFILES_COL_MD5.getName());
			this.colSha1 = rs.findColumn(SQL_TBL_MEDIAFILES_COL_SHA1.getName());
			this.colModified = rs.findColumn(SQL_TBL_MEDIAFILES_COL_DMODIFIED.getName());
			this.colEnabled = rs.findColumn(SQL_TBL_MEDIAFILES_COL_ENABLED.getName());
			this.colEnabledModified = rs.findColumn(SQL_TBL_MEDIAFILES_COL_ENABLEDMODIFIED.getName());
			this.colMissing = rs.findColumn(SQL_TBL_MEDIAFILES_COL_MISSING.getName());
			this.colId = rs.findColumn(SQL_TBL_MEDIAFILES_COL_ID.getName());
			this.colRemLoc = rs.findColumn(SQL_TBL_MEDIAFILES_COL_REMLOC.getName());
			this.colStartCount = rs.findColumn(SQL_TBL_MEDIAFILES_COL_STARTCNT.getName());
			this.colEndCount = rs.findColumn(SQL_TBL_MEDIAFILES_COL_ENDCNT.getName());
			this.colDuration = rs.findColumn(SQL_TBL_MEDIAFILES_COL_DURATION.getName());
			this.colLastPlay = rs.findColumn(SQL_TBL_MEDIAFILES_COL_DLASTPLAY.getName());
			this.colWidth = rs.findColumn(SQL_TBL_MEDIAFILES_COL_WIDTH.getName());
			this.colHeight = rs.findColumn(SQL_TBL_MEDIAFILES_COL_HEIGHT.getName());
		}
	}

	protected static List<MediaItem> local_parseRecordSet (final ResultSet rs, final DefaultMediaItemFactory itemFactory) throws SQLException {
		final List<MediaItem> ret = new ArrayList<>();
		if (rs.next()) {
			final ColumnIndexes indexes = new ColumnIndexes(rs);
			do {
				ret.add(createMediaItem(rs, indexes, itemFactory));
			}
			while (rs.next());
		}
		return ret;
	}

	protected static MediaItem createMediaItem (final ResultSet rs, final ColumnIndexes indexes, final DefaultMediaItemFactory itemFactory) throws SQLException {
		final String filePath = rs.getString(indexes.colFile);
		final MediaItem mi = itemFactory.getNewMediaItem(filePath);

		/* The object returned by the itemFactory may not be fresh.
		 * It is important that this method call every possible setter.
		 * Any setter not called will result in stale data remaining.
		 * Not using .reset() as that would not be thread safe.
		 */

		final int i = rs.getInt(indexes.colType);
		final MediaType t = MediaType.parseInt(i);
		mi.setMediaType(t);

		mi.setMimeType(rs.getString(indexes.colMimeType));

		mi.setDateAdded(SqliteHelper.readDate(rs, indexes.colAdded));

		final byte[] md5Bytes = rs.getBytes(indexes.colMd5);
		mi.setMd5(md5Bytes == null ? null : new BigInteger(md5Bytes));

		final byte[] sha1Bytes = rs.getBytes(indexes.colSha1);
		mi.setSha1(sha1Bytes == null ? null : new BigInteger(sha1Bytes));

		mi.setDateLastModified(SqliteHelper.readDate(rs, indexes.colModified));
		mi.setEnabled(rs.getInt(indexes.colEnabled) != 0, // default to true.
				SqliteHelper.readDate(rs, indexes.colEnabledModified));
		mi.setMissing(rs.getInt(indexes.colMissing) == 1); // default to false.
		mi.setDbRowId(rs.getLong(indexes.colId));
		mi.setRemoteLocation(rs.getString(indexes.colRemLoc));

		if (t == MediaType.TRACK) {
			mi.setStartCount(rs.getLong(indexes.colStartCount));
			mi.setEndCount(rs.getLong(indexes.colEndCount));
			mi.setDuration(rs.getInt(indexes.colDuration));
			mi.setDateLastPlayed(SqliteHelper.readDate(rs, indexes.colLastPlay));
		}
		else {
			mi.setStartCount(0); // TODO extract constant for default value.
			mi.setEndCount(0); // TODO extract constant for default value.
			mi.setDuration(0); // TODO extract constant for default value.
			mi.setDateLastPlayed(null); // TODO extract constant for default value.
		}

		if (t == MediaType.PICTURE) {
			mi.setWidth(rs.getInt(indexes.colWidth));
			mi.setHeight(rs.getInt(indexes.colHeight));
		}
		else {
			mi.setWidth(0); // TODO extract constant for default value.
			mi.setHeight(0); // TODO extract constant for default value.
		}

		return mi;
	}

	@Override
	public Collection<MediaAlbum> getAlbums () throws DbException {
		try (final Statement stat = getDbCon().createStatement()) {
			try (final ResultSet rs = stat.executeQuery(SQL_TBL_ALBUMS_Q_ALL)) {
				final List<MediaAlbum> ret = new ArrayList<>();
				while (rs.next()) {
					final long rowId = rs.getLong(SQL_TBL_ALBUMS_COL_ROWID);
					final String name = rs.getString(SQL_TBL_ALBUMS_COL_NAME);
					final int trackCount = rs.getInt("track_count");
					ret.add(new MediaAlbumImpl(rowId, name, trackCount));
				}
				return ret;
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public MediaAlbum createAlbum (final String name) throws DbException {
		MediaAlbum album = getAlbum(name);
		if (album != null) return album;
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUMS_ADD)) {
			ps.setString(1, name);
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for createAlbum('" + name + "').");
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
		album = getAlbum(name);
		if (album == null) throw new DbException("Failed to find album that was just created: '" + name + "'.");
		return album;
	}

	@Override
	public void removeAlbum (final MediaAlbum album) throws DbException {
		// TODO remove missing items.
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUMS_REMOVE)) {
			ps.setLong(1, album.getDbRowId());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured.");
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	/**
	 * Get album, or null if not found.
	 */
	@Override
	public MediaAlbum getAlbum (final String qName) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUMS_Q_GET)) {
			ps.setString(1, qName);
			try (final ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				final long rowId = rs.getLong(SQL_TBL_ALBUMS_COL_ROWID);
				final String name = rs.getString(SQL_TBL_ALBUMS_COL_NAME);
				final int trackCount = rs.getInt("track_count");
				return new MediaAlbumImpl(rowId, name, trackCount);
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	private boolean _albumHasItem (final MediaAlbum album, final IDbItem item) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUM_ITEMS_Q_HAS)) {
			ps.setLong(1, album.getDbRowId());
			ps.setLong(2, item.getDbRowId());
			try (final ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return true;
				}
				return false;
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void addToAlbum (final MediaAlbum album, final IDbItem item) throws DbException {
		if (_albumHasItem(album, item)) return;
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUM_ITEMS_ADD)) {
			ps.setLong(1, album.getDbRowId());
			ps.setLong(2, item.getDbRowId());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for addToAlbum(" + album.getDbRowId() + "," + item.getDbRowId() + ").");
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeFromAlbum (final MediaAlbum album, final IDbItem item) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUM_ITEMS_REMOVE)) {
			ps.setLong(1, album.getDbRowId());
			ps.setLong(2, item.getDbRowId());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for removeFromAlbum(" + album.getDbRowId() + "," + item.getDbRowId() + ").");
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public int removeFromAllAlbums (final IDbItem item) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_ALBUM_ITEMS_REMOVE_FROM_ALL)) {
			ps.setLong(1, item.getDbRowId());
			return ps.executeUpdate();
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<String> getSources () throws DbException {
		try (final Statement stat = getDbCon().createStatement()) {
			try (final ResultSet rs = stat.executeQuery(SQL_TBL_SOURCES_Q_ALL)) {
				final List<String> ret = new ArrayList<>();
				while (rs.next()) {
					ret.add(rs.getString("path"));
				}
				return ret;
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void addSource (final String source) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_SOURCES_ADD)) {
			ps.setString(1, source);
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured for addSource('"+source+"').");
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void removeSource (final String source) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_SOURCES_REMOVE)) {
			ps.setString(1, source);
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured for removeSource('"+source+"').");
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

}
