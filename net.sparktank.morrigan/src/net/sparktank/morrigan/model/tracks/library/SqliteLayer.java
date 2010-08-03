package net.sparktank.morrigan.model.tracks.library;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.tags.MediaTag;
import net.sparktank.morrigan.model.tags.MediaTagClassification;
import net.sparktank.morrigan.model.tags.MediaTagClassificationFactory;
import net.sparktank.morrigan.model.tags.MediaTagType;
import net.sparktank.sqlitewrapper.DbException;
import net.sparktank.sqlitewrapper.GenericSqliteLayer;

public class SqliteLayer extends GenericSqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory.
	
	public static final DbConFactory FACTORY = new DbConFactory();
	
	public static class DbConFactory extends RecyclingFactory<SqliteLayer, String, Void, DbException> {
		
		DbConFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(SqliteLayer product) {
			return true;
		}
		
		@Override
		protected SqliteLayer makeNewProduct(String material) throws DbException {
			return new SqliteLayer(material);
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	SqliteLayer (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Persisted params.
	
	public void setProp (String key, String value) throws DbException {
		try {
			local_setProp(key, value);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
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
//	DB readers.
	
	public List<MediaLibraryTrack> updateListOfAllMedia (List<MediaLibraryTrack> list, LibrarySort sort, LibrarySortDirection direction, boolean hideMissing) throws DbException {
		try {
			return local_updateListOfAllMedia(list, sort, direction, hideMissing);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public List<MediaLibraryTrack> getAllMedia (LibrarySort sort, LibrarySortDirection direction, boolean hideMissing) throws DbException {
		try {
			return local_getAllMedia(sort, direction, hideMissing);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public List<MediaLibraryTrack> simpleSearch (String term, String esc, int maxResults) throws DbException {
		try {
			return local_simpleSearch(term, esc, maxResults);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public List<String> getSources () throws DbException {
		try {
			return local_getSources();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public boolean hasTags (long rowId) throws DbException {
		try {
			return local_hasTags(rowId);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public boolean hasTag (long rowId, String tag, MediaTagType type, MediaTagClassification mtc) throws DbException {
		try {
			return local_hasTag(rowId, tag, type, mtc);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public List<MediaTag> getTags (long rowId) throws DbException {
		try {
			return local_getTags(rowId);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public List<MediaTagClassification> getTagClassifications () throws DbException {
		try {
			return local_getTagClassifications();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB writers.
	
	public void addSource (String source) throws DbException {
		try {
			local_addSource(source);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void removeSource (String source) throws DbException {
		try {
			local_removeSource(source);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	/**
	 * 
	 * @param file
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	public boolean addFile (File file) throws DbException {
		try {
			return local_addTrack(file.getAbsolutePath(), file.lastModified());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	/**
	 * 
	 * @param filepath
	 * @param lastModified
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	public boolean addFile (String filepath, long lastModified) throws DbException {
		try {
			return local_addTrack(filepath, lastModified);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public int removeFile (String sfile) throws DbException {
		try {
			return local_removeTrack(sfile);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public int removeFile (long rowId) throws DbException {
		try {
			return local_removeTrack(rowId);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	/**
	 * Inc start count by one and set last played date.
	 * @param sfile
	 * @throws DbException
	 */
	public void incTrackPlayed (String sfile) throws DbException {
		try {
			local_incTrackStartCnt(sfile, 1);
			local_setDateLastPlayed(sfile, new Date());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void incTrackFinished (String sfile) throws DbException {
		try {
			local_incTrackEndCnt(sfile, 1);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void incTrackStartCnt (String sfile, long n) throws DbException {
		try {
			local_incTrackStartCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void setTrackStartCnt (String sfile, long n) throws DbException {
		try {
			local_setTrackStartCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void incTrackEndCnt (String sfile, long n) throws DbException {
		try {
			local_incTrackEndCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void setTrackEndCnt (String sfile, long n) throws DbException {
		try {
			local_setTrackEndCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void setDateLastPlayed (String sfile, Date date) throws DbException {
		try {
			local_setDateLastPlayed(sfile, date);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void setDateAdded (String sfile, Date date) throws DbException {
		try {
			local_setDateAdded(sfile, date);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void setTrackDuration (String sfile, int duration) throws DbException {
		try {
			local_setTrackDuration(sfile, duration);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void setHashcode (String sfile, long hashcode) throws DbException {
		try {
			local_setHashCode(sfile, hashcode);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void setDateLastModified (String sfile, Date date) throws DbException {
		try {
			local_setDateLastModified(sfile, date);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void setEnabled (String sfile, boolean value) throws DbException {
		try {
			local_setEnabled(sfile, value);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void setMissing (String sfile, boolean value) throws DbException {
		try {
			local_setMissing(sfile, value);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void setRemoteLocation (String sfile, String remoteLocation) throws DbException {
		try {
			local_setRemoteLocation(sfile, remoteLocation);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public boolean addTag (long mf_rowId, String tag, MediaTagType type, MediaTagClassification mtc) throws DbException {
		try {
			return local_addTag(mf_rowId, tag, type, mtc);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public boolean addTag (long mf_rowId, String tag, MediaTagType type, String mtc) throws DbException {
		try {
			return local_addTag(mf_rowId, tag, type, mtc);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void moveTags (long from_mf_rowId, long to_mf_rowId) throws DbException {
		try {
			local_moveTags(from_mf_rowId, to_mf_rowId);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void removeTag (MediaTag tag) throws DbException {
		try {
			local_removeTag(tag);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void clearTags (long mf_rowId) throws DbException {
		try {
			local_clearTags(mf_rowId);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void addTagClassification (String classificationName) throws DbException {
		try {
			local_addTagClassification(classificationName);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Schema.
	
	/* sqlite change notes:
	 * 
	 * ALTER TABLE tbl_mediafiles ADD COLUMN dmodified DATETIME;
	 * ALTER TABLE tbl_mediafiles ADD COLUMN sremloc VARCHAR(1000);
	 * .schema tbl_mediafiles
	 * 
	 */
	
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
	 * tbl_mediafiles
	 */
	
	private static final String SQL_TBL_MEDIAFILES_EXISTS = 
		"SELECT name FROM sqlite_master WHERE name='tbl_mediafiles';";
	
	private static final String SQL_TBL_MEDIAFILES_CREATE = 
		"create table tbl_mediafiles(" +
	    "sfile VARCHAR(1000) not null collate nocase primary key," +
	    "dadded DATETIME," +
	    "lstartcnt INT(6)," +
	    "lendcnt INT(6)," +
	    "dlastplay DATETIME," +
	    "lmd5 BIGINT," +
	    "dmodified DATETIME," +
	    "lduration INT(6)," +
	    "benabled INT(1)," +
	    "bmissing INT(1)," +
	    "sremloc VARCHAR(1000) NOT NULL" +
	    ");";
	
	private static final String SQL_TBL_MEDIAFILES_COL_ROWID = "ROWID"; // sqlite automatically creates this.
	private static final String SQL_TBL_MEDIAFILES_COL_FILE = "sfile";
	private static final String SQL_TBL_MEDIAFILES_COL_DADDED = "dadded";
	private static final String SQL_TBL_MEDIAFILES_COL_STARTCNT = "lstartcnt";
	private static final String SQL_TBL_MEDIAFILES_COL_ENDCNT = "lendcnt";
	private static final String SQL_TBL_MEDIAFILES_COL_DLASTPLAY = "dlastplay";
	private static final String SQL_TBL_MEDIAFILES_COL_DURATION = "lduration";
	private static final String SQL_TBL_MEDIAFILES_COL_HASHCODE = "lmd5";
	private static final String SQL_TBL_MEDIAFILES_COL_DMODIFIED = "dmodified";
	private static final String SQL_TBL_MEDIAFILES_COL_ENABLED = "benabled";
	private static final String SQL_TBL_MEDIAFILES_COL_MISSING = "bmissing";
	private static final String SQL_TBL_MEDIAFILES_COL_REMLOC = "sremloc";
	
	private static final String SQL_TBL_SOURCES_EXISTS =
		"SELECT name FROM sqlite_master WHERE name='tbl_sources';";
	
	private static final String SQL_TBL_SOURCES_CREATE = 
		"CREATE TABLE tbl_sources (" +
		"path VARCHAR(1000) NOT NULL collate nocase primary key" +
		");";
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Properties.
	
	private static final String SQL_TBL_PROP_Q_GET =
		"SELECT value FROM tbl_prop WHERE key=?";
	
	private static final String SQL_TBL_PROP_Q_INSERT =
		"INSERT INTO tbl_prop (key,value) VALUES (?,?)";
	
	private static final String SQL_TBL_PROP_Q_UPDATE =
		"UPDATE tbl_prop SET value=? WHERE key=?";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sources.
	
	private static final String SQL_TBL_SOURCES_Q_ALL =
		"SELECT path FROM tbl_sources ORDER BY path ASC";
	
	private static final String SQL_TBL_SOURCES_ADD =
		"INSERT INTO tbl_sources (path) VALUES (?)";
	
	private static final String SQL_TBL_SOURCES_REMOVE =
		"DELETE FROM tbl_sources WHERE path=?";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Library queries.
	
	private static final String SQL_TBL_MEDIAFILES_Q_ALL = 
		"SELECT ROWID, sfile, dadded, lstartcnt, lendcnt, dlastplay," +
	    " lmd5, dmodified, lduration, benabled, bmissing, sremloc" +
	    " FROM tbl_mediafiles" +
	    " ORDER BY {COL} {DIR};";
	
	private static final String SQL_TBL_MEDIAFILES_Q_NOTMISSING = 
		"SELECT ROWID, sfile, dadded, lstartcnt, lendcnt, dlastplay," +
		" lmd5, dmodified, lduration, benabled, bmissing, sremloc" +
		" FROM tbl_mediafiles" +
		" WHERE (bmissing<>1 OR bmissing is NULL)" +
		" ORDER BY {COL} {DIR};";
	
	private static final String SQL_TBL_MEDIAFILES_Q_SIMPLESEARCH = 
		"SELECT ROWID, sfile, dadded, lstartcnt, lendcnt, dlastplay," +
		" lmd5, dmodified, lduration, benabled, bmissing, sremloc" +
	    " FROM tbl_mediafiles" +
	    " WHERE sfile LIKE ? ESCAPE ?" +
	    " AND (bmissing<>1 OR bmissing is NULL) AND (benabled<>0 OR benabled is NULL)" +
	    " ORDER BY dlastplay DESC, lendcnt DESC, lstartcnt DESC, sfile COLLATE NOCASE ASC;";
	
	private static final String SQL_TBL_MEDIAFILES_Q_EXISTS =
		"SELECT count(*) FROM tbl_mediafiles WHERE sfile=? COLLATE NOCASE;";
	
	private static final String SQL_TBL_MEDIAFILES_ADD =
		"INSERT INTO tbl_mediafiles (sfile,dadded,lstartcnt,lendcnt,dmodified,lduration,benabled,bmissing,sremloc) VALUES" +
		" (?,?,0,0,?,-1,1,0,'');";
	
	private static final String SQL_TBL_MEDIAFILES_REMOVE =
		"DELETE FROM tbl_mediafiles WHERE sfile=?";
	
	private static final String SQL_TBL_MEDIAFILES_REMOVE_BYROWID =
		"DELETE FROM tbl_mediafiles WHERE ROWID=?";
	
	private static final String SQL_TBL_MEDIAFILES_INCSTART =
		"UPDATE tbl_mediafiles SET lstartcnt=lstartcnt+?" +
        " WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_SETSTART =
		"UPDATE tbl_mediafiles SET lstartcnt=? WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_INCEND =
		"UPDATE tbl_mediafiles SET lendcnt=lendcnt+?" +
		" WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_SETEND =
		"UPDATE tbl_mediafiles SET lendcnt=? WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_SETDATELASTPLAYED =
		"UPDATE tbl_mediafiles SET dlastplay=?" +
		" WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_SETDATEADDED =
		"UPDATE tbl_mediafiles SET dadded=?" +
		" WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_SETDURATION =
		"UPDATE tbl_mediafiles SET lduration=?" +
		" WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_SETHASHCODE =
		"UPDATE tbl_mediafiles SET lmd5=?" +
		" WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_SETDMODIFIED =
		"UPDATE tbl_mediafiles SET dmodified=?" +
		" WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_SETENABLED =
		"UPDATE tbl_mediafiles SET benabled=?" +
		" WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_SETMISSING =
		"UPDATE tbl_mediafiles SET bmissing=?" +
		" WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_SETREMLOC =
		"UPDATE tbl_mediafiles SET sremloc=?" +
		" WHERE sfile=?;";
	
	public enum LibrarySort { 
		FILE      {@Override public String toString() { return "file path";       } },
		STARTCNT  {@Override public String toString() { return "start count";     } },
		ENDCNT    {@Override public String toString() { return "end count";       } },
		DADDED    {@Override public String toString() { return "date added";      } },
		DLASTPLAY {@Override public String toString() { return "last played";     } },
		HASHCODE  {@Override public String toString() { return "hashcode";        } },
		DMODIFIED {@Override public String toString() { return "date modified";   } },
		DURATION  {@Override public String toString() { return "duration";        } }
		};
	
	public enum LibrarySortDirection { ASC, DESC };
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Tag queries.
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Superclass methods.
	
	@Override
	protected SqlCreateCmd[] getTblCreateCmds() {
		return new SqlCreateCmd[] {
				new SqlCreateCmd(SQL_TBL_PROP_EXISTS, SQL_TBL_PROP_CREATE),
				new SqlCreateCmd(SQL_TBL_MEDIAFILES_EXISTS, SQL_TBL_MEDIAFILES_CREATE),
				new SqlCreateCmd(SQL_TBL_SOURCES_EXISTS, SQL_TBL_SOURCES_CREATE),
				new SqlCreateCmd(SQL_TBL_TAGS_EXISTS, SQL_TBL_TAGS_CREATE),
				new SqlCreateCmd(SQL_TBL_TAGCLS_EXISTS, SQL_TBL_TAGCLS_CREATE)
				};
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Properties.
	
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
			
		} finally {
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
//	Sources.
	
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
//	Media.
	
	private SimpleDateFormat SQL_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private List<MediaLibraryTrack> local_updateListOfAllMedia (List<MediaLibraryTrack> list, LibrarySort sort, LibrarySortDirection direction, boolean hideMissing) throws SQLException, ClassNotFoundException {
		String sql = local_getAllMediaSql(sort, direction, hideMissing);
		ResultSet rs;
		
		List<MediaLibraryTrack> ret;
		PreparedStatement ps = getDbCon().prepareStatement(sql);
		try {
			rs = ps.executeQuery();
			try {
				ret = local_parseAndUpdateFromRecordSet(list, rs);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
	private List<MediaLibraryTrack> local_getAllMedia (LibrarySort sort, LibrarySortDirection direction, boolean hideMissing) throws SQLException, ClassNotFoundException {
		String sql = local_getAllMediaSql(sort, direction, hideMissing);
		ResultSet rs;
		
		List<MediaLibraryTrack> ret;
		PreparedStatement ps = getDbCon().prepareStatement(sql);
		try {
			rs = ps.executeQuery();
			try {
				ret = local_parseRecordSet(rs);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
	private String local_getAllMediaSql (LibrarySort sort, LibrarySortDirection direction, boolean hideMissing) {
		String sql;
		
		if (hideMissing) {
			sql = SQL_TBL_MEDIAFILES_Q_NOTMISSING;
		} else {
			sql = SQL_TBL_MEDIAFILES_Q_ALL;
		}
		
		switch (direction) {
			case ASC:
				sql = sql.replace("{DIR}", "ASC");
				break;
				
			case DESC:
				sql = sql.replace("{DIR}", "DESC");
				break;
				
			default:
				throw new IllegalArgumentException();
				
		}
		
		switch (sort) {
			case FILE:
				sql = sql.replace("{COL}", SQL_TBL_MEDIAFILES_COL_FILE + " COLLATE NOCASE");
				break;
			
			case DADDED:
				sql = sql.replace("{COL}", SQL_TBL_MEDIAFILES_COL_DADDED);
				break;
			
			case STARTCNT:
				sql = sql.replace("{COL}", SQL_TBL_MEDIAFILES_COL_STARTCNT);
				break;
				
			case ENDCNT:
				sql = sql.replace("{COL}", SQL_TBL_MEDIAFILES_COL_ENDCNT);
				break;
				
			case DLASTPLAY:
				sql = sql.replace("{COL}", SQL_TBL_MEDIAFILES_COL_DLASTPLAY);
				break;
				
			case HASHCODE:
				sql = sql.replace("{COL}", SQL_TBL_MEDIAFILES_COL_HASHCODE);
				break;
				
			case DMODIFIED:
				sql = sql.replace("{COL}", SQL_TBL_MEDIAFILES_COL_DMODIFIED);
				break;
				
			case DURATION:
				sql = sql.replace("{COL}", SQL_TBL_MEDIAFILES_COL_DURATION);
				break;
				
			default:
				throw new IllegalArgumentException();
		}
		
		return sql;
	}
	
	private List<MediaLibraryTrack> local_simpleSearch (String term, String esc, int maxResults) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		List<MediaLibraryTrack> ret;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_Q_SIMPLESEARCH);
		try {
			ps.setString(1, "%" + term + "%");
			ps.setString(2, esc);
			
			if (maxResults > 0) {
				ps.setMaxRows(maxResults);
			}
			
			rs = ps.executeQuery();
			try {
				ret = local_parseRecordSet(rs);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
	private List<MediaLibraryTrack> local_parseAndUpdateFromRecordSet (List<MediaLibraryTrack> list, ResultSet rs) throws SQLException {
		List<MediaLibraryTrack> finalList = new ArrayList<MediaLibraryTrack>();
		
		// Build a HashMap of existing items to make lookup a lot faster.
		Map<String, MediaLibraryTrack> keepMap = new HashMap<String, MediaLibraryTrack>(list.size());
		for (MediaLibraryTrack e : list) {
			keepMap.put(e.getFilepath(), e);
		}
		
		/* Extract entry from DB.  Compare it to existing entries and
		 * create new list as we go. 
		 */
		while (rs.next()) {
			MediaLibraryTrack newItem = new MediaLibraryTrack();
			newItem.setFilepath(rs.getString(SQL_TBL_MEDIAFILES_COL_FILE));
			newItem.setDbRowId(rs.getLong(SQL_TBL_MEDIAFILES_COL_ROWID));
			newItem.setDateAdded(readDate(rs, SQL_TBL_MEDIAFILES_COL_DADDED));
			newItem.setStartCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_STARTCNT));
			newItem.setEndCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_ENDCNT));
			newItem.setDateLastPlayed(readDate(rs, SQL_TBL_MEDIAFILES_COL_DLASTPLAY));
			newItem.setDuration(rs.getInt(SQL_TBL_MEDIAFILES_COL_DURATION));
			newItem.setHashcode(rs.getLong(SQL_TBL_MEDIAFILES_COL_HASHCODE));
			newItem.setDateLastModified(readDate(rs, SQL_TBL_MEDIAFILES_COL_DMODIFIED));
			newItem.setEnabled(rs.getInt(SQL_TBL_MEDIAFILES_COL_ENABLED) != 0); // default to true.
			newItem.setMissing(rs.getInt(SQL_TBL_MEDIAFILES_COL_MISSING) == 1); // default to false.
			newItem.setRemoteLocation(rs.getString(SQL_TBL_MEDIAFILES_COL_REMLOC));
			
			MediaLibraryTrack oldItem = keepMap.get(newItem.getFilepath());
			if (oldItem != null) {
				oldItem.setFromMediaItem(newItem);
				finalList.add(oldItem);
			} else {
				finalList.add(newItem);
			}
		}
		
		return finalList;
	}
	
	private List<MediaLibraryTrack> local_parseRecordSet (ResultSet rs) throws SQLException {
		List<MediaLibraryTrack> ret = new ArrayList<MediaLibraryTrack>();
		
		while (rs.next()) {
			MediaLibraryTrack mt = new MediaLibraryTrack();
			mt.setFilepath(rs.getString(SQL_TBL_MEDIAFILES_COL_FILE));
			mt.setDbRowId(rs.getLong(SQL_TBL_MEDIAFILES_COL_ROWID));
			mt.setDateAdded(readDate(rs, SQL_TBL_MEDIAFILES_COL_DADDED));
			mt.setStartCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_STARTCNT));
			mt.setEndCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_ENDCNT));
			mt.setDateLastPlayed(readDate(rs, SQL_TBL_MEDIAFILES_COL_DLASTPLAY));
			mt.setDuration(rs.getInt(SQL_TBL_MEDIAFILES_COL_DURATION));
			mt.setHashcode(rs.getLong(SQL_TBL_MEDIAFILES_COL_HASHCODE));
			mt.setDateLastModified(readDate(rs, SQL_TBL_MEDIAFILES_COL_DMODIFIED));
			mt.setEnabled(rs.getInt(SQL_TBL_MEDIAFILES_COL_ENABLED) != 0); // default to true.
			mt.setMissing(rs.getInt(SQL_TBL_MEDIAFILES_COL_MISSING) == 1); // default to false.
			mt.setRemoteLocation(rs.getString(SQL_TBL_MEDIAFILES_COL_REMLOC));
			
			ret.add(mt);
		}
		
		return ret;
	}
	
	private boolean local_addTrack (String filePath, long lastModified) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ResultSet rs;
		
		int n;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_Q_EXISTS);
		try {
			ps.setString(1, filePath);
			rs = ps.executeQuery();
			try {
				n = 0;
				if (rs.next()) {
					n = rs.getInt(1);
				}
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
		
		if (n == 0) {
			System.err.println("Adding file '" + filePath + "' to '"+getDbFilePath()+"'.");
			ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_ADD);
			try {
				ps.setString(1, filePath);
				ps.setDate(2, new java.sql.Date(new Date().getTime()));
				ps.setDate(3, new java.sql.Date(lastModified));
				n = ps.executeUpdate();
			} finally {
				ps.close();
			}
			if (n<1) throw new DbException("No update occured for addTrack('"+filePath+"','"+lastModified+"').");
			
			return true;
		}
		
		return false;
	}
	
	private int local_removeTrack (String sfile) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		int ret;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_REMOVE);
		try {
			ps.setString(1, sfile);
			ret = ps.executeUpdate();
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
	private int local_removeTrack (long rowId) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		int ret;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_REMOVE_BYROWID);
		try {
			ps.setLong(1, rowId);
			ret = ps.executeUpdate();
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
	private void local_setDateAdded (String sfile, Date date) throws Exception, ClassNotFoundException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDATEADDED);
		int n;
		try {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_incTrackStartCnt (String sfile, long x) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCSTART);
		int n;
		try {
			ps.setLong(1, x);
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_setTrackStartCnt (String sfile, long x) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETSTART);
		int n;
		try {
			ps.setLong(1, x);
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_setDateLastPlayed (String sfile, Date date) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDATELASTPLAYED);
		int n;
		try {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_incTrackEndCnt (String sfile, long x) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCEND);
		int n;
		try {
			ps.setLong(1, x);
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_setTrackEndCnt (String sfile, long x) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETEND);
		int n;
		try {
			ps.setLong(1, x);
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_setTrackDuration (String sfile, int duration) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDURATION);
		int n;
		try {
			ps.setInt(1, duration);
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_setHashCode (String sfile, long hashcode) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETHASHCODE);
		int n;
		try {
			ps.setLong(1, hashcode);
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_setDateLastModified (String sfile, Date date) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDMODIFIED);
		int n;
		try {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_setEnabled (String sfile, boolean value) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETENABLED);
		int n;
		try {
			ps.setInt(1, value ? 1 : 0);
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_setMissing (String sfile, boolean value) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETMISSING);
		int n;
		try {
			ps.setInt(1, value ? 1 : 0);
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_setRemoteLocation(String sfile, String remoteLocation) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETREMLOC);
		int n;
		try {
			ps.setString(1, remoteLocation);
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for setRemoteLocation('"+sfile+"','"+remoteLocation+"').");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Tags.
	
	private boolean local_addTag (long mf_rowId, String tag, MediaTagType type, String cls_name) throws SQLException, ClassNotFoundException, DbException {
		MediaTagClassification mtc = local_getTagClassification(cls_name);
		if (mtc == null) {
			mtc = local_addTagClassification(cls_name);
		}
		return local_addTag(mf_rowId, tag, type, mtc);
	}
	
	private boolean local_addTag (long mf_rowId, String tag, MediaTagType type, MediaTagClassification mtc) throws SQLException, ClassNotFoundException, DbException {
		if (local_hasTag(mf_rowId, tag, type, mtc)) {
			return false;
		}
		
		if (mtc != null) {
			local_addTag(mf_rowId, tag, type, mtc.getRowId());
		} else {
			local_addTag(mf_rowId, tag, type, 0);
		}
		return true;
	}
	
	private void local_addTag (long mf_rowId, String tag, MediaTagType type, long cls_rowid) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_TAGS_ADD);
		int n;
		try {
			ps.setLong(1, mf_rowId);
			ps.setString(2, tag);
			ps.setInt(3, type.getIndex());
			if (cls_rowid > 0 ) {
				ps.setLong(4, cls_rowid);
			} else {
				ps.setNull(4, java.sql.Types.INTEGER);
			}
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_moveTags (long from_mf_rowId, long to_mf_rowId) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_TAGS_MOVE);
		int n;
		try {
			ps.setLong(1, to_mf_rowId);
			ps.setLong(2, from_mf_rowId);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for moveTags('"+from_mf_rowId+"' to '"+to_mf_rowId+"').");
	}
	
	private void local_removeTag(MediaTag tag) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_TAGS_REMOVE);
		int n;
		try {
			ps.setLong(1, tag.getDbRowId());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_clearTags(long mf_rowId) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_TAGS_CLEAR);
		int n;
		try {
			ps.setLong(1, mf_rowId);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for clearTags('"+mf_rowId+"').");
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
			return local_hasTag(mf_rowId, tag, type, mtc.getRowId());
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
					
					MediaTag mt = new MediaTag(rowId, tag, mtt, mtc);
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
	
	/**
	 * This method will read the date from the DB
	 * weather it was stored as a number or a string.
	 * Morrigan uses the number method, but terra used strings.
	 * Using this method allows for backward compatability.
	 */
	private Date readDate (ResultSet rs, String column) throws SQLException {
		java.sql.Date date = rs.getDate(column);
		if (date!=null) {
			long time = date.getTime();
			if (time > 100000) { // If the date was stored old-style, we get back the year :S.
				return new Date(time);
			}
			
			String s = rs.getString(column);
			try {
				Date d = this.SQL_DATE.parse(s);
				return d;
			} catch (Exception e) {/*Can't really do anything with this error anyway.*/}
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
