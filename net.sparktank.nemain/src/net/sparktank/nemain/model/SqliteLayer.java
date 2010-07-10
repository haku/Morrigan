package net.sparktank.nemain.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.sqlitewrapper.DbException;
import net.sparktank.sqlitewrapper.GenericSqliteLayer;

public class SqliteLayer extends GenericSqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public SqliteLayer (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	SQL.
	
	static private final String SQL_TBL_EVENTS_EXISTS = "SELECT name FROM sqlite_master WHERE name='tbl_events';";
	static private final String SQL_TBL_EVENTS_CREATE = "CREATE TABLE tbl_events (entry TEXT,date_year INT,date_month INT,date_day INT);";
	
	private static final String SQL_TBL_EVENTS_COL_ENTRY = "entry";
	private static final String SQL_TBL_EVENTS_COL_YEAR = "date_year";
	private static final String SQL_TBL_EVENTS_COL_MONTH = "date_month";
	private static final String SQL_TBL_EVENTS_COL_DAY = "date_day";
	
	static private final String SQL_TBL_EVENTS_Q_ALL =
		"SELECT entry,date_year,date_month,date_day FROM tbl_events ORDER BY date_year ASC, date_month ASC, date_day ASC;";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Super class methods.
	
	@Override
	protected SqlCreateCmd[] getTblCreateCmds() {
		return new SqlCreateCmd[] { new SqlCreateCmd(SQL_TBL_EVENTS_EXISTS, SQL_TBL_EVENTS_CREATE) };
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public methods.
	
	public List<NemainEvent> getEvents () throws DbException {
		try {
			return _getEvents();
		} catch (SQLException e) {
			throw new DbException(e);
		} catch (ClassNotFoundException e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Data fetching methods.
	
	private List<NemainEvent> _getEvents () throws SQLException, ClassNotFoundException {
		List<NemainEvent> ret;
		Statement stat = getDbCon().createStatement();
		try {
			ResultSet rs = stat.executeQuery(SQL_TBL_EVENTS_Q_ALL);
			try {
				ret = _getEvents_parseRecordSet(rs);
			} finally {
				rs.close();
			}
		} finally {
			stat.close();
		}
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private List<NemainEvent> _getEvents_parseRecordSet (ResultSet rs) throws SQLException {
		List<NemainEvent> ret = new LinkedList<NemainEvent>();
		
		while (rs.next()) {
			String entry = rs.getString(SQL_TBL_EVENTS_COL_ENTRY);
			int year = rs.getInt(SQL_TBL_EVENTS_COL_YEAR);
			int month = rs.getInt(SQL_TBL_EVENTS_COL_MONTH);
			int day = rs.getInt(SQL_TBL_EVENTS_COL_DAY);
			
			NemainEvent mtc = new NemainEvent(entry, year, month, day);
			ret.add(mtc);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
