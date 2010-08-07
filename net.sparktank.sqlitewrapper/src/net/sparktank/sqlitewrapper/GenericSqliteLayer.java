package net.sparktank.sqlitewrapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public abstract class GenericSqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Instance properties.
	
	private final String dbFilePath;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	protected GenericSqliteLayer (String dbFilePath) throws DbException {
		this.dbFilePath = dbFilePath;
		
		try {
			initDatabaseTables();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		dispose();
		super.finalize();
	}
	
	public void dispose () throws DbException {
		try {
			disposeDbCon();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Properties.
	
	public String getDbFilePath() {
		return this.dbFilePath;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB connection.
	
	private Connection dbConnection = null;
	
	protected Connection getDbCon () throws ClassNotFoundException, SQLException {
		if (this.dbConnection==null) {
			Class.forName("org.sqlite.JDBC");
			String url = "jdbc:sqlite:/" + this.dbFilePath;
			this.dbConnection = DriverManager.getConnection(url);
		}
		
		return this.dbConnection;
	}
	
	private void disposeDbCon () throws SQLException {
		if (this.dbConnection!=null) {
			this.dbConnection.close();
		}
	}
	
	public void setAutoCommit (boolean b) throws DbException {
		try {
			getDbCon().setAutoCommit(b);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void commit () throws DbException {
		try {
			getDbCon().commit();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public void rollback () throws DbException {
		try {
			getDbCon().rollback();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Init.
	
	private void initDatabaseTables () throws SQLException, ClassNotFoundException {
		Statement stat = getDbCon().createStatement();
		
		List<SqlCreateCmd> tblCreateCmds = getTblCreateCmds();
		
		for (SqlCreateCmd sqlCreateCmd : tblCreateCmds) {
			try {
				ResultSet rs;
				rs = stat.executeQuery(sqlCreateCmd.getTblExistsSql());
				try {
					if (!rs.next()) { // True if there are rows in the result.
						stat.executeUpdate(sqlCreateCmd.getTblCreateSql());
					}
				}
				catch (SQLException e) {
					System.err.println("SQLException while executing '"+sqlCreateCmd.getTblCreateSql()+"'.");
					throw e;
				}
				finally {
					rs.close();
				}
			}
			finally {
				stat.close();
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public class SqlCreateCmd {
		
		private String tblExistsSql;
		private String tblCreateSql;
		
		public SqlCreateCmd (String tblExistsSql, String tblCreateSql) {
			this.tblExistsSql = tblExistsSql;
			this.tblCreateSql = tblCreateSql;
		}
		
		public String getTblExistsSql() {
			return this.tblExistsSql;
		}
		public void setTblExistsSql(String tblExistsSql) {
			this.tblExistsSql = tblExistsSql;
		}
		
		public String getTblCreateSql() {
			return this.tblCreateSql;
		}
		public void setTblCreateSql(String tblCreateSql) {
			this.tblCreateSql = tblCreateSql;
		}
		
	}
	
	protected abstract List<SqlCreateCmd> getTblCreateCmds ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

}
