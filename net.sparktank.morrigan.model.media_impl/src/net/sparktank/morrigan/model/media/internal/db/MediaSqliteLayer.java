package net.sparktank.morrigan.model.media.internal.db;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.model.db.IDbItem;
import net.sparktank.morrigan.model.media.IMediaItem;
import net.sparktank.morrigan.model.media.IMediaItemStorageLayer;
import net.sparktank.morrigan.model.media.IMediaItemStorageLayerChangeListener;
import net.sparktank.morrigan.model.media.MediaTag;
import net.sparktank.morrigan.model.media.MediaTagClassification;
import net.sparktank.morrigan.model.media.MediaTagType;
import net.sparktank.morrigan.model.media.internal.MediaTagClassificationFactory;
import net.sparktank.morrigan.model.media.internal.MediaTagImpl;
import net.sparktank.sqlitewrapper.DbException;
import net.sparktank.sqlitewrapper.GenericSqliteLayer;

public abstract class MediaSqliteLayer<T extends IMediaItem> extends GenericSqliteLayer implements IMediaItemStorageLayer<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	protected MediaSqliteLayer (String dbFilePath, boolean autoCommit) throws DbException {
		super(dbFilePath, autoCommit);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final Collection<IMediaItemStorageLayerChangeListener<T>> changeListeners = new LinkedList<IMediaItemStorageLayerChangeListener<T>>();
	
	@Override
	public void addChangeListener(IMediaItemStorageLayerChangeListener<T> listener) {
		// TODO rewrite this to use a map instead?
		if (!this.changeListeners.contains(listener)) this.changeListeners.add(listener);
	}
	
	@Override
	public void removeChangeListener(IMediaItemStorageLayerChangeListener<T> listener) {
		this.changeListeners.remove(listener);
	}
	
	private final IMediaItemStorageLayerChangeListener<T> changeCaller = new IMediaItemStorageLayerChangeListener<T> () {
		
		@Override
		public void propertySet(String key, String value) {
			for (IMediaItemStorageLayerChangeListener<T> l : MediaSqliteLayer.this.changeListeners) {
				l.propertySet(key, value);
			}
		}
		
		@Override
		public void mediaItemAdded(String filePath) {
			for (IMediaItemStorageLayerChangeListener<T> l : MediaSqliteLayer.this.changeListeners) {
				l.mediaItemAdded(filePath);
			}
		}
		
		@Override
		public void mediaItemsAdded(List<File> files) {
			for (IMediaItemStorageLayerChangeListener<T> l : MediaSqliteLayer.this.changeListeners) {
				l.mediaItemsAdded(files);
			}
		}
		
		@Override
		public void mediaItemRemoved(String filePath) {
			for (IMediaItemStorageLayerChangeListener<T> l : MediaSqliteLayer.this.changeListeners) {
				l.mediaItemRemoved(filePath);
			}
		}
		
		@Override
		public void mediaItemUpdated(String filePath) {
			for (IMediaItemStorageLayerChangeListener<T> l : MediaSqliteLayer.this.changeListeners) {
				l.mediaItemUpdated(filePath);
			}
		}
		
		@Override
		public void mediaItemTagAdded(IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) {
			for (IMediaItemStorageLayerChangeListener<T> l : MediaSqliteLayer.this.changeListeners) {
				l.mediaItemUpdated(null); // TODO pass-through actual file.
			}
		}
		
		@Override
		public void mediaItemTagsMoved(IDbItem from_item, IDbItem to_item) {
			for (IMediaItemStorageLayerChangeListener<T> l : MediaSqliteLayer.this.changeListeners) {
				l.mediaItemUpdated(null); // TODO pass-through actual file.
			}
		}
		
		@Override
		public void mediaItemTagRemoved(MediaTag tag) {
			for (IMediaItemStorageLayerChangeListener<T> l : MediaSqliteLayer.this.changeListeners) {
				l.mediaItemUpdated(null); // TODO pass-through actual file.
			}
		}
		
		@Override
		public void mediaItemTagsCleared(IDbItem item) {
			for (IMediaItemStorageLayerChangeListener<T> l : MediaSqliteLayer.this.changeListeners) {
				l.mediaItemUpdated(null); // TODO pass-through actual file.
			}
		}
		
	};
	
	protected IMediaItemStorageLayerChangeListener<T> getChangeCaller () {
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
		"mf_rowid INT," +
		"tag VARCHAR(100)," +
		"type INT," +
		"cls_rowid INT" +
		");";
	
	private static final String SQL_TBL_TAGS_COL_ROWID = "ROWID";
//	private static final String SQL_TBL_TAGS_COL_MEDIAFILEROWID = "mf_rowid";
	private static final String SQL_TBL_TAGS_COL_TAG = "tag";
	private static final String SQL_TBL_TAGS_COL_TYPE = "type";
	private static final String SQL_TBL_TAGS_COL_CLSROWID = "cls_rowid";
	
	/* - - - - - - - - - - - - - - - -
	 * tbl_tag_class
	 */
	
	private static final String SQL_TBL_TAGCLS_EXISTS = 
		"SELECT name FROM sqlite_master WHERE name='tbl_tag_cls';";
	
	private static final String SQL_TBL_TAGCLS_CREATE = 
		"CREATE TABLE tbl_tag_cls (" +
		"cls VARCHAR(100) not null collate nocase primary key" +
		");";
	
	private static final String SQL_TBL_TAGCLS_COL_ROWID = "ROWID";
	private static final String SQL_TBL_TAGCLS_COL_CLS = "cls";
	
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
		"INSERT INTO tbl_tags (mf_rowid,tag,type,cls_rowid) VALUES (?,?,?,?);";
	
	private static final String SQL_TBL_TAGS_MOVE =
		"UPDATE tbl_tags SET mf_rowid=? WHERE mf_rowid=?;";
	
	private static final String SQL_TBL_TAGS_REMOVE =
		"DELETE FROM tbl_tags WHERE ROWID=?;";
	
	private static final String SQL_TBL_TAGS_CLEAR =
		"DELETE FROM tbl_tags WHERE mf_rowid=?;";
	
	private static final String SQL_TBL_TAGS_Q_HASANY =
		"SELECT ROWID FROM tbl_tags WHERE mf_rowid=?;";
	
	private static final String SQL_TBL_TAGS_Q_ALL =
		"SELECT t.ROWID,t.tag,t.type,t.cls_rowid,c.cls" +
		" FROM tbl_tags AS t LEFT OUTER JOIN tbl_tag_cls AS c ON t.cls_rowid=c.ROWID" +
		" WHERE t.mf_rowid=?" +
		" ORDER BY t.type ASC, c.cls ASC, t.tag ASC;";
	
	private static final String SQL_TBL_TAGS_Q_HASTAG =
		"SELECT ROWID FROM tbl_tags WHERE mf_rowid=? AND tag=? AND type=? AND cls_rowid=?;";
	
	private static final String SQL_TBL_TAGS_Q_HASTAG_CLSNULL =
		"SELECT ROWID FROM tbl_tags WHERE mf_rowid=? AND tag=? AND type=? AND cls_rowid IS NULL;";
	
	private static final String SQL_TBL_TAGCLS_ADD =
		"INSERT INTO tbl_tag_cls (cls) VALUES (?);";
	
	private static final String SQL_TBL_TAGCLS_Q_ALL =
		"SELECT ROWID,cls FROM tbl_tag_cls;";
	
	private static final String SQL_TBL_TAGCLS_Q_CLS =
		"SELECT ROWID,cls FROM tbl_tag_cls WHERE cls=?;";
	
	private static final String SQL_TBL_TAGCLS_Q_ROWID =
		"SELECT ROWID,cls FROM tbl_tag_cls WHERE ROWID=?;";
	
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
		List<SqlCreateCmd> l = new LinkedList<SqlCreateCmd>();
		
		l.add(new SqlCreateCmd(SQL_TBL_PROP_EXISTS, SQL_TBL_PROP_CREATE));
		
		l.add(new SqlCreateCmd(SQL_TBL_TAGS_EXISTS, SQL_TBL_TAGS_CREATE));
		l.add(new SqlCreateCmd("SELECT name FROM sqlite_master WHERE name='tags_idx';", "CREATE INDEX tags_idx ON tbl_tags(mf_rowid,tag);")); // TODO extract strings.
		
		l.add(new SqlCreateCmd(SQL_TBL_TAGCLS_EXISTS, SQL_TBL_TAGCLS_CREATE));
		l.add(new SqlCreateCmd("SELECT name FROM sqlite_master WHERE name='tag_cls_idx';", "CREATE INDEX tag_cls_idx ON tbl_tag_cls(cls);")); // TODO extract strings.
		
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
		MediaTagClassification mtc = local_getTagClassification(cls_name);
		if (mtc == null) {
			mtc = local_addTagClassification(cls_name);
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
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_TAGS_MOVE);
		int n;
		try {
			ps.setLong(1, to_item.getDbRowId());
			ps.setLong(2, from_item.getDbRowId());
			
			n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured for moveTags('"+from_item+"' to '"+to_item+"').");
			
			this.changeCaller.mediaItemTagsMoved(from_item, to_item);
		}
		finally {
			ps.close();
		}
	}
	
	private void local_removeTag(MediaTag tag) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_TAGS_REMOVE);
		int n;
		try {
			ps.setLong(1, tag.getDbRowId());
			n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			
			this.changeCaller.mediaItemTagRemoved(tag);
		}
		finally {
			ps.close();
		}
	}
	
	private void local_clearTags(IDbItem item) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_TAGS_CLEAR);
		int n;
		try {
			ps.setLong(1, item.getDbRowId());
			n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured for clearTags('"+item+"').");
			
			this.changeCaller.mediaItemTagsCleared(item);
		}
		finally {
			ps.close();
		}
	}
	
	private boolean local_hasTags (long mf_rowId) throws SQLException, ClassNotFoundException {
		ResultSet rs;
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_Q_HASANY);
		
		try {
			ps.setLong(1, mf_rowId);
			rs = ps.executeQuery();
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
		
		ResultSet rs;
		PreparedStatement ps = getDbCon().prepareStatement(sql);
		try {
			ps.setLong(1, mf_rowId);
			ps.setString(2, tag);
			ps.setInt(3, type.getIndex());
			if (cls_rowid > 0 ) {
				ps.setLong(4, cls_rowid);
			}
			rs = ps.executeQuery();
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
		List<MediaTag> ret = new LinkedList<MediaTag>();
		ResultSet rs;
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_TAGS_Q_ALL);
		
		try {
			ps.setLong(1, mf_rowId);
			rs = ps.executeQuery();
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
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_TAGCLS_ADD);
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
			throw new DbException("Query for TagClassification clsRowId='"+clsRowId+"' returned more than one result.");
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
	
	private List<MediaTagClassification> local_getTagClassification_parseRecordSet (ResultSet rs) throws SQLException {
		List<MediaTagClassification> ret = new LinkedList<MediaTagClassification>();
		
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
