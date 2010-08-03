package net.sparktank.nemain.model;

import java.sql.PreparedStatement;
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
	
	static private final String SQL_TBL_EVENTS_Q_EXISTS =
		"SELECT ROWID FROM tbl_events WHERE date_year=? AND date_month=? AND date_day=?;";
	
	static private final String SQL_TBL_EVENTS_ADD =
		"INSERT INTO tbl_events (entry,date_year,date_month,date_day) VALUES (?,?,?,?);";
	
	static private final String SQL_TBL_EVENTS_UPDATE =
		"UPDATE tbl_events SET entry=? WHERE date_year=? AND date_month=? AND date_day=?;";
	
	static private final String SQL_TBL_EVENTS_DELETE =
		"DELETE FROM tbl_events WHERE date_year=? AND date_month=? AND date_day=?;";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Super class methods.
	
	@Override
	protected List<SqlCreateCmd> getTblCreateCmds() {
		List<SqlCreateCmd> l = new LinkedList<SqlCreateCmd>();
		
		l.add(new SqlCreateCmd(SQL_TBL_EVENTS_EXISTS, SQL_TBL_EVENTS_CREATE));
		
		return l;
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
	
	public void setEvent (NemainEvent event) throws DbException {
		try {
			_setEvent(event);
		} catch (SQLException e) {
			throw new DbException(e);
		} catch (ClassNotFoundException e) {
			throw new DbException(e);
		} catch (DbException e) {
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
	
	private boolean _isEventExists (NemainDate date) throws SQLException, ClassNotFoundException {
		ResultSet rs;
		PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_EVENTS_Q_EXISTS);
		try {
			ps.setInt(1, date.getYear());
			ps.setInt(2, date.getMonth());
			ps.setInt(3, date.getDay());
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Data fetching helper methods.
	
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
//	Data writing methods.
	
	private void _setEvent (NemainEvent event) throws SQLException, ClassNotFoundException, DbException {
		if (_isEventExists(event)) {
			if (event.getEntryText() == null || event.getEntryText().length() < 1) {
				_deleteEvent(event);
			} else {
				if (event.getEntryText() != null && event.getEntryText().length() > 0) {
					_updateEvent(event);
				} else {
					throw new IllegalArgumentException("Can't create empty entry.");
				}
			}
		}
		else {
			_addEvent(event);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Data writing helper methods.
	
	private void _addEvent (NemainEvent event) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_EVENTS_ADD);
		int n;
		try {
			ps.setString(1, event.getEntryText());
			ps.setInt(2, event.getYear());
			ps.setInt(3, event.getMonth());
			ps.setInt(4, event.getDay());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for _addEvent('"+event.toString()+"').");
	}
	
	private void _updateEvent (NemainEvent event) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_EVENTS_UPDATE);
		int n;
		try {
			ps.setString(1, event.getEntryText());
			ps.setInt(2, event.getYear());
			ps.setInt(3, event.getMonth());
			ps.setInt(4, event.getDay());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for _updateEvent('"+event.toString()+"').");
	}
	
	private void _deleteEvent (NemainDate event) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_EVENTS_DELETE);
		int n;
		try {
			ps.setInt(1, event.getYear());
			ps.setInt(2, event.getMonth());
			ps.setInt(3, event.getDay());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for _updateEvent('"+event.toString()+"').");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
