package net.sparktank.morrigan.library;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.model.media.MediaTrack;

public class SqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String dbFilePath;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public.
	
//	TODO: nice, exception-wrapped public methods go here.
	
	public SqliteLayer (String dbFilePath) throws DbException {
		this.dbFilePath = dbFilePath;
		
		try {
			initDatabaseTables();
		} catch (SQLException e) {
			throw new DbException(e);
		} catch (ClassNotFoundException e) {
			throw new DbException(e);
		}
	}
	
	public void dispose () throws DbException {
		try {
			disposeDbCon();
		} catch (SQLException e) {
			throw new DbException(e);
		}
	}
	
	public List<MediaTrack> getAllMedia () throws DbException {
		try {
			return query_getAllMedia();
		} catch (SQLException e) {
			throw new DbException(e);
		} catch (ClassNotFoundException e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Schema.
	
	private static final String SQL_TBL_MEDIAFILES_EXISTS = 
		"SELECT name FROM sqlite_master WHERE name='tbl_mediafiles'";
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queries.
	
	private static final String SQL_TBL_MEDIAFILES_Q_ALL = 
		"SELECT sfile, dadded, lstartcnt, lendcnt, dlastplay, " +
	    "lmd5, lduration, benabled, bmissing FROM tbl_mediafiles " +
	    "ORDER BY sfile COLLATE NOCASE ASC;";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB connection.
	
	private Connection dbConnection = null;
	
	private Connection getDbCon () throws ClassNotFoundException, SQLException {
		if (dbConnection==null) {
			Class.forName("org.sqlite.JDBC");
			String url = "jdbc:sqlite:/" + dbFilePath;
			System.out.println("url=" + url);
			dbConnection = DriverManager.getConnection(url); // FIXME is this always safe?
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
		
		ResultSet rs = stat.executeQuery(SQL_TBL_MEDIAFILES_EXISTS);
		if (!rs.next()) { // True if there are rows in the result.
			stat.executeUpdate(SQL_TBL_MEDIAFILES_CREATE);
		}
		
		rs.close();
		stat.close();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Query.
	
	private List<MediaTrack> query_getAllMedia () throws SQLException, ClassNotFoundException {
		Statement stat = getDbCon().createStatement();
		ResultSet rs = stat.executeQuery(SQL_TBL_MEDIAFILES_Q_ALL);
		
		List<MediaTrack> ret = new ArrayList<MediaTrack>();
		
		while (rs.next()) {
			MediaTrack mt = new MediaTrack();
			
			mt.setfilepath(rs.getString("sfile"));
			
			ret.add(mt);
		}
		
		rs.close();
		stat.close();
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
