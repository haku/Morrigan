package net.sparktank.morrigan.library;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.sparktank.morrigan.var.Const;

public class SqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public.
	
//	TODO: nice, exception-wrapped public methods go here.
	
	public SqliteLayer () throws DbException {
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
//	DB connection.
	
	private Connection dbConnection = null;
	
	private Connection getDbCon () throws ClassNotFoundException, SQLException {
		if (dbConnection==null) {
			Class.forName("org.sqlite.JDBC");
			dbConnection = DriverManager.getConnection("jdbc:sqlite:" + Const.SQLITE_DBNAME);
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
		if (!rs.first()) { // True if there are rows in the result.
			stat.executeUpdate(SQL_TBL_MEDIAFILES_CREATE);
		}
		
		rs.close();
		stat.close();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
