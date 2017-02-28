package com.vaguehope.sqlitewrapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public abstract class GenericSqliteLayer implements IGenericDbLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String DRIVER_CLASS = "org.sqlite.JDBC";

	private static final String PRAGMA_FK_SET = "PRAGMA foreign_keys = ON;";
	private static final String PRAGMA_FK_GET = "PRAGMA foreign_keys;";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Instance properties.

	private final String dbFilePath;
	private final boolean autoCommit;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.

	protected GenericSqliteLayer (final String dbFilePath, final boolean autoCommit) throws DbException {
		this.dbFilePath = dbFilePath;
		this.autoCommit = autoCommit;

		try {
			initDatabaseTables();
			if (!autoCommit) commitOrRollBack();
		} catch (Exception e) {
			throw new DbException("Failed to initialise database tables for db '" + dbFilePath + "'.", e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		dispose();
		super.finalize();
	}

	@Override
	public void dispose () {
		try {
			disposeDbCon();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Properties.

	@Override
	public String getDbFilePath() {
		return this.dbFilePath;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB connection.

	private Connection dbConnection = null; // FIXME should be thread local?

	private Connection makeConnection() throws ClassNotFoundException, SQLException {
		Class.forName(DRIVER_CLASS);

		final String url;
		if (this.dbFilePath.startsWith(":memory:") || this.dbFilePath.startsWith("file:")) {
			url = "jdbc:sqlite:" + this.dbFilePath;
		}
		else {
			url = "jdbc:sqlite:/" + this.dbFilePath;
		}

		Connection con = DriverManager.getConnection(url);
		if (con == null) throw new IllegalStateException("DriverManager returned null connection object for " + url + ".");

		// Setup environment.
		Statement stmt = con.createStatement();
		try {
			stmt.execute(PRAGMA_FK_SET);
			ResultSet rs = stmt.executeQuery(PRAGMA_FK_GET);
			try {
				rs.next();
				int fk = rs.getInt(1);
				if (fk != 1) throw new UnsupportedOperationException("Call had no effect: " + PRAGMA_FK_SET + " (v=" + fk + ")");
			} finally { rs.close(); }
		} finally { stmt.close(); }

		con.setAutoCommit(this.autoCommit);
//		System.err.println("AutoCommit=" + con.getAutoCommit() + " for '" + this.dbFilePath + "'.");

		return con;
	}

	protected Connection getDbCon () throws ClassNotFoundException, SQLException {
		// TODO FIXME make this thread-safe using atomic objects.
		if (this.dbConnection == null || this.dbConnection.isClosed()) {
			this.dbConnection = makeConnection();
		}
//		else if (!this.dbConnection.isValid(10)) {
//			this.dbConnection.close();
//			this.dbConnection = makeConnection();
//		}

		if (this.dbConnection == null) throw new IllegalArgumentException("Failed to make driver class instance.");

		return this.dbConnection;
	}

	private void disposeDbCon () throws SQLException {
		if (this.dbConnection != null) {
			this.dbConnection.close();
		}
	}

	@Override
	public void commitOrRollBack () throws DbException {
		if (this.autoCommit) throw new IllegalStateException("Can not commit auto-commit connection.");
		try {
			getDbCon().commit();
		}
		catch (Exception e) {
			rollback();
			throw new DbException(e);
		}
	}

	@Override
	public void rollback () throws DbException {
		if (this.autoCommit) throw new IllegalStateException("Can not rollback auto-commit connection.");
		try {
			getDbCon().rollback();
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Initialise.

	private void initDatabaseTables () throws SQLException, ClassNotFoundException {
		Statement stmt = getDbCon().createStatement();
		try {
			for (SqlCreateCmd sqlCreateCmd : getTblCreateCmds()) {
				try {
					ResultSet rs;
					rs = stmt.executeQuery(sqlCreateCmd.getTblExistsSql());
					try {
						if (!rs.next()) { // True if there are rows in the result.
							stmt.executeUpdate(sqlCreateCmd.getTblCreateSql());
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
					stmt.close();
				}
			}
		}
		finally {
			stmt.close();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static class SqlCreateCmd {

		private final String tblExistsSql;
		private final String tblCreateSql;

		public SqlCreateCmd (final String tblExistsSql, final String tblCreateSql) {
			this.tblExistsSql = tblExistsSql;
			this.tblCreateSql = tblCreateSql;
		}

		public String getTblExistsSql() {
			return this.tblExistsSql;
		}

		public String getTblCreateSql() {
			return this.tblCreateSql;
		}

	}

	protected abstract List<SqlCreateCmd> getTblCreateCmds ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

}
