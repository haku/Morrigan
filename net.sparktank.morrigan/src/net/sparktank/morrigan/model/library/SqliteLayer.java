package net.sparktank.morrigan.model.library;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;

public class SqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String dbFilePath;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public SqliteLayer (String dbFilePath) throws DbException {
		this.dbFilePath = dbFilePath;
		
		try {
			initDatabaseTables();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void dispose () throws DbException {
		try {
			disposeDbCon();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Propeties.
	
	public String getDbFilePath() {
		return dbFilePath;
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
	
	public List<MediaLibraryItem> getAllMedia (LibrarySort sort, LibrarySortDirection direction, boolean hideMissing) throws DbException {
		try {
			return local_getAllMedia(sort, direction, hideMissing);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public List<MediaLibraryItem> simpleSearch (String term, String esc, int maxResults) throws DbException {
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Schema.
	
	/* sqlite notes:
	 * Add dmodified column:
	 * ALTER TABLE tbl_mediafiles ADD COLUMN dmodified DATETIME;
	 * ALTER TABLE tbl_mediafiles ADD COLUMN sremloc VARCHAR(1000);
	 * .schema tbl_mediafiles
	 */
	
	private static final String SQL_TBL_PROP_EXISTS = 
		"SELECT name FROM sqlite_master WHERE name='tbl_prop';";
	
	private static final String SQL_TBL_PROP_CREATE = 
		"CREATE TABLE tbl_prop (" +
		"key VARCHAR(100) NOT NULL collate nocase primary key," +
		"value VARCHAR(1000)" +
		");";
	
	private static final String SQL_TBL_PROP_COL_VALUE = "value";
	
	private static final String SQL_TBL_MEDIAFILES_EXISTS = 
		"SELECT name FROM sqlite_master WHERE name='tbl_mediafiles';";
	
	private static final String SQL_TBL_MEDIAFILES_CREATE = 
		"create table tbl_mediafiles(" +
	    "sfile VARCHAR(10000) not null collate nocase primary key," +
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
		"path VARCHAR(1000) NOT NULL  collate nocase primary key" +
		");";
	
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
//	DB connection.
	
	private Connection dbConnection = null;
	
	private Connection getDbCon () throws ClassNotFoundException, SQLException {
		if (dbConnection==null) {
			Class.forName("org.sqlite.JDBC");
			String url = "jdbc:sqlite:/" + dbFilePath; // FIXME is this always safe?
			dbConnection = DriverManager.getConnection(url);
		}
		
		return dbConnection;
	}
	
	private void disposeDbCon () throws SQLException {
		if (dbConnection!=null) {
			dbConnection.close();
		}
	}
	
	public void setAutoCommit (boolean b) throws MorriganException {
		try {
			getDbCon().setAutoCommit(b);
		} catch (Exception e) {
			throw new MorriganException(e);
		}
	}
	
	public void commit () throws MorriganException {
		try {
			getDbCon().commit();
		} catch (Exception e) {
			throw new MorriganException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Init.
	
	private void initDatabaseTables () throws SQLException, ClassNotFoundException {
		Statement stat = getDbCon().createStatement();
		try {
			ResultSet rs;
			
			rs = stat.executeQuery(SQL_TBL_PROP_EXISTS);
			try {
				if (!rs.next()) { // True if there are rows in the result.
					stat.executeUpdate(SQL_TBL_PROP_CREATE);
				}
			} finally {
				rs.close();
			}
			
			rs = stat.executeQuery(SQL_TBL_MEDIAFILES_EXISTS);
			try {
				if (!rs.next()) { // True if there are rows in the result.
					stat.executeUpdate(SQL_TBL_MEDIAFILES_CREATE);
				}
			} finally {
				rs.close();
			}
			
			rs = stat.executeQuery(SQL_TBL_SOURCES_EXISTS);
			try {
				if (!rs.next()) { // True if there are rows in the result.
					stat.executeUpdate(SQL_TBL_SOURCES_CREATE);
				}
			} finally {
				rs.close();
			}
			
		} finally {
			stat.close();
		}
	}
	
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
			ps.close();
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
		if (n<1) throw new DbException("No update occured.");
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
		if (n<1) throw new DbException("No update occured.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media.
	
	private SimpleDateFormat SQL_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private List<MediaLibraryItem> local_getAllMedia (LibrarySort sort, LibrarySortDirection direction, boolean hideMissing) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		
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
		
		List<MediaLibraryItem> ret;
		ps = getDbCon().prepareStatement(sql);
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
	
	private List<MediaLibraryItem> local_simpleSearch (String term, String esc, int maxResults) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		List<MediaLibraryItem> ret;
		
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
	
	private List<MediaLibraryItem> local_parseRecordSet (ResultSet rs) throws SQLException {
		List<MediaLibraryItem> ret = new ArrayList<MediaLibraryItem>();
		
		while (rs.next()) {
			MediaLibraryItem mt = new MediaLibraryItem();
			
			mt.setDbRowId(rs.getLong(SQL_TBL_MEDIAFILES_COL_ROWID));
			
			mt.setFilepath(rs.getString(SQL_TBL_MEDIAFILES_COL_FILE));
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
			if (n<1) throw new DbException("No update occured.");
			
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
		try {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_incTrackStartCnt (String sfile, long n) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCSTART);
		try {
			ps.setLong(1, n);
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_setTrackStartCnt (String sfile, long n) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETSTART);
		try {
			ps.setLong(1, n);
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_setDateLastPlayed (String sfile, Date date) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDATELASTPLAYED);
		try {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_incTrackEndCnt (String sfile, long n) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCEND);
		try {
			ps.setLong(1, n);
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_setTrackEndCnt (String sfile, long n) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETEND);
		try {
			ps.setLong(1, n);
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_setTrackDuration (String sfile, int duration) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDURATION);
		try {
			ps.setInt(1, duration);
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_setHashCode (String sfile, long hashcode) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETHASHCODE);
		try {
			ps.setLong(1, hashcode);
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_setDateLastModified (String sfile, Date date) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDMODIFIED);
		try {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_setEnabled (String sfile, boolean value) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETENABLED);
		try {
			ps.setInt(1, value ? 1 : 0);
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_setMissing (String sfile, boolean value) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETMISSING);
		try {
			ps.setInt(1, value ? 1 : 0);
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
	}
	
	private void local_setRemoteLocation(String sfile, String remoteLocation) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETREMLOC);
		try {
			ps.setString(1, remoteLocation);
			ps.setString(2, sfile);
			ps.executeUpdate();
		} finally {
			ps.close();
		}
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
			} else {
				String s = rs.getString(column);
				try {
					Date d = SQL_DATE.parse(s);
					return d;
				} catch (Exception e) {}
			}
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
