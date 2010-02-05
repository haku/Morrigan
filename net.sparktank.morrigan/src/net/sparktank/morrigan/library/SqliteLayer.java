package net.sparktank.morrigan.library;

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

import net.sparktank.morrigan.model.media.MediaTrack;

public class SqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String dbFilePath;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public.
	
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
	
	public List<MediaTrack> getAllMedia (LibrarySort sort, LibrarySortDirection direction) throws DbException {
		try {
			return local_getAllMedia(sort, direction);
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
			return local_addTrack(file);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void incTrackStartCnt (String sfile) throws DbException {
		try {
			local_incTrackStartCnt(sfile);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void incTrackEndCnt (String sfile) throws DbException {
		try {
			local_incTrackEndCnt(sfile);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Schema.
	
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
	    "lduration INT(6)," +
	    "benabled INT(1)," +
	    "bmissing INT(1));";
	
	private static final String SQL_TBL_MEDIAFILES_COL_FILE = "sfile";
	private static final String SQL_TBL_MEDIAFILES_COL_DADDED = "dadded";
	private static final String SQL_TBL_MEDIAFILES_COL_STARTCNT = "lstartcnt";
	private static final String SQL_TBL_MEDIAFILES_COL_ENDCNT = "lendcnt";
	private static final String SQL_TBL_MEDIAFILES_COL_DLASTPLAY = "dlastplay";
	
	private static final String SQL_TBL_SOURCES_EXISTS =
		"SELECT name FROM sqlite_master WHERE name='tbl_sources';";
	
	private static final String SQL_TBL_SOURCES_CREATE = 
		"CREATE TABLE tbl_sources (" +
		"path VARCHAR(1000) NOT NULL  collate nocase primary key" +
		");";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sources.
	
	private static final String SQL_TBL_SOURCES_Q_ALL =
		"SELECT path FROM tbl_sources;";
	
	private static final String SQL_TBL_SOURCES_ADD =
		"INSERT INTO tbl_sources (path) VALUES (?)";
	
	private static final String SQL_TBL_SOURCES_REMOVE =
		"DELETE FROM tbl_sources WHERE path=?";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Library queries.
	
	private static final String SQL_TBL_MEDIAFILES_Q_ALL = 
		"SELECT sfile, dadded, lstartcnt, lendcnt, dlastplay," +
	    "lmd5, lduration, benabled, bmissing FROM tbl_mediafiles" +
	    " ORDER BY {COL} {DIR};";
	
	private static final String SQL_TBL_MEDIAFILES_Q_EXISTS =
		"SELECT count(*) FROM tbl_mediafiles WHERE sfile=? COLLATE NOCASE;";
	
	private static final String SQL_TBL_MEDIAFILES_ADD =
		"INSERT INTO tbl_mediafiles (sfile,dadded,lstartcnt,lendcnt,lduration,benabled) VALUES" +
		" (?,?,0,0,0,1);";
	
	private static final String SQL_TBL_MEDIAFILES_INCSTART =
		"UPDATE tbl_mediafiles SET lstartcnt=lstartcnt+1,dlastplay=?" +
        " WHERE sfile=?;";
	
	private static final String SQL_TBL_MEDIAFILES_INCEND =
		"UPDATE tbl_mediafiles SET lendcnt=lendcnt+1" +
		" WHERE sfile=?;";
	
	public enum LibrarySort { FILE, DADDED, STARTCNT, ENDCNT, DLASTPLAY };
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Init.
	
	private void initDatabaseTables () throws SQLException, ClassNotFoundException {
		Statement stat = getDbCon().createStatement();
		
		ResultSet rs;
		
		rs = stat.executeQuery(SQL_TBL_MEDIAFILES_EXISTS);
		if (!rs.next()) { // True if there are rows in the result.
			stat.executeUpdate(SQL_TBL_MEDIAFILES_CREATE);
		}
		rs.close();
		
		rs = stat.executeQuery(SQL_TBL_SOURCES_EXISTS);
		if (!rs.next()) { // True if there are rows in the result.
			stat.executeUpdate(SQL_TBL_SOURCES_CREATE);
		}
		rs.close();
		
		stat.close();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Sources.
	
	private List<String> local_getSources () throws SQLException, ClassNotFoundException {
		Statement stat = getDbCon().createStatement();
		ResultSet rs = stat.executeQuery(SQL_TBL_SOURCES_Q_ALL);
		
		List<String> ret = new ArrayList<String>();
		
		while (rs.next()) {
			ret.add(rs.getString("path"));
		}
		
		rs.close();
		stat.close();
		
		return ret;
	}
	
	private void local_addSource (String source) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_SOURCES_ADD);
		ps.setString(1, source);
		int n = ps.executeUpdate();
		ps.close();
		if (n<1) throw new DbException("No update occured.");
	}
	
	private void local_removeSource (String source) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_SOURCES_REMOVE);
		ps.setString(1, source);
		int n = ps.executeUpdate();
		ps.close();
		if (n<1) throw new DbException("No update occured.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media.
	
	private SimpleDateFormat SQL_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private List<MediaTrack> local_getAllMedia (LibrarySort sort, LibrarySortDirection direction) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		
		String sql = SQL_TBL_MEDIAFILES_Q_ALL;
		
		switch (direction) {
			case ASC:
				sql = sql.replace("{DIR}", "ASC");
				break;
				
			case DESC:
				sql = sql.replace("{DIR}", "DESC");
				break;
				
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
				
			default:
				throw new IllegalArgumentException();
		}
		
		ps = getDbCon().prepareStatement(sql);
		rs = ps.executeQuery();
		
		List<MediaTrack> ret = new ArrayList<MediaTrack>();
		
		while (rs.next()) {
			MediaTrack mt = new MediaTrack();
			
			mt.setfilepath(rs.getString(SQL_TBL_MEDIAFILES_COL_FILE));
			mt.setDateAdded(readDate(rs, SQL_TBL_MEDIAFILES_COL_DADDED));
			mt.setStartCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_STARTCNT));
			mt.setEndCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_ENDCNT));
			mt.setDateLastPlayed(readDate(rs, SQL_TBL_MEDIAFILES_COL_DLASTPLAY));
			
			ret.add(mt);
		}
		
		rs.close();
		ps.close();
		
		return ret;
	}
	
	private boolean local_addTrack (File file) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ResultSet rs;
		
		String filePath = file.getAbsolutePath();
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_Q_EXISTS);
		ps.setString(1, filePath);
		rs = ps.executeQuery();
		int n = 0;
		if (rs.next()) {
			n = rs.getInt(1);
		}
		rs.close();
		ps.close();
		
		if (n == 0) {
			ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_ADD);
			ps.setString(1, filePath);
			ps.setDate(2, new java.sql.Date(new Date().getTime()));
			n = ps.executeUpdate();
			ps.close();
			if (n<1) throw new DbException("No update occured.");
			
			return true;
		}
		
		return false;
	}
	
	private void local_incTrackStartCnt (String sfile) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCSTART);
		ps.setDate(1, new java.sql.Date(new Date().getTime()));
		ps.setString(2, sfile);
		ps.executeUpdate();
		
		ps.close();
	}
	
	private void local_incTrackEndCnt (String sfile) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCEND);
		ps.setString(1, sfile);
		ps.executeUpdate();
		
		ps.close();
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
