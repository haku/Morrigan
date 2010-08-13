package net.sparktank.morrigan.model.tracks.library;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.MediaSqliteLayer2;
import net.sparktank.morrigan.model.db.impl.DbColumn;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.morrigan.model.tracks.MediaTrack;
import net.sparktank.sqlitewrapper.DbException;

public class LibrarySqliteLayer2 extends MediaSqliteLayer2<MediaTrack> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory.
	
	public static final DbConFactory FACTORY = new DbConFactory();
	
	public static class DbConFactory extends RecyclingFactory<LibrarySqliteLayer2, String, Void, DbException> {
		
		DbConFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(LibrarySqliteLayer2 product) {
			return true;
		}
		
		@Override
		protected LibrarySqliteLayer2 makeNewProduct(String material) throws DbException {
			return new LibrarySqliteLayer2(material);
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	LibrarySqliteLayer2 (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public methods.
	
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
	
	public void setTrackDuration (String sfile, int duration) throws DbException {
		try {
			local_setTrackDuration(sfile, duration);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_STARTCNT =  new DbColumn("lstartcnt", "start count", "INT(6)",   "0");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_ENDCNT =    new DbColumn("lendcnt",   "end count",   "INT(6)",   "0");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_DLASTPLAY = new DbColumn("dlastplay", "last played", "DATETIME", null);
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_DURATION =  new DbColumn("lduration", "duration",    "INT(6)",   "-1");
	
	public static final DbColumn[] SQL_TBL_MEDIAFILES_COLS2 = new DbColumn[] {
		SQL_TBL_MEDIAFILES_COL_STARTCNT,
		SQL_TBL_MEDIAFILES_COL_ENDCNT,
		SQL_TBL_MEDIAFILES_COL_DLASTPLAY,
		SQL_TBL_MEDIAFILES_COL_DURATION,
		};
	
	static public IDbColumn parseColumnFromName (String name) {
		for (IDbColumn c : SQL_TBL_MEDIAFILES_COLS) {
			if (c.getName().equals(name)) {
				return c;
			}
		}
		for (IDbColumn c : SQL_TBL_MEDIAFILES_COLS2) {
			if (c.getName().equals(name)) {
				return c;
			}
		}
		throw new IllegalArgumentException("Can't parse '"+name+"'.");
	}
	
	@Override
	protected List<DbColumn> generateSqlTblMediaFilesColumns() {
		List<DbColumn> l = super.generateSqlTblMediaFilesColumns();
		for (DbColumn c : SQL_TBL_MEDIAFILES_COLS2) {
			l.add(c);
		}
		return l;
	}
	
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
	
	private static final String SQL_TBL_MEDIAFILES_SETDURATION =
		"UPDATE tbl_mediafiles SET lduration=?" +
		" WHERE sfile=?;";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Types.
	
	@Override
	protected MediaTrack getNewT() {
		return new MediaTrack();
	}
	
	@Override
	protected void setTFromRs(MediaTrack item, ResultSet rs) throws SQLException {
		item.setStartCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_STARTCNT.getName()));
		item.setEndCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_ENDCNT.getName()));
		item.setDuration(rs.getInt(SQL_TBL_MEDIAFILES_COL_DURATION.getName()));
		item.setDateLastPlayed(readDate(rs, SQL_TBL_MEDIAFILES_COL_DLASTPLAY.getName()));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
