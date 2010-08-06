package net.sparktank.morrigan.model;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sparktank.morrigan.helpers.GeneratedString;
import net.sparktank.sqlitewrapper.DbException;

/*
 * TODO FIXME Extract common code between this and GallerySqliteLayer.
 */
public abstract class MediaSqliteLayer2<T extends MediaItem> extends MediaSqliteLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	protected MediaSqliteLayer2 (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Types.
	
	protected abstract T getNewT ();
	
	protected  abstract void setTFromRs (T item, ResultSet rs) throws SQLException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB readers.
	
	public List<T> updateListOfAllMedia (List<T> list, DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		try {
			return local_updateListOfAllMedia(list, sort, direction, hideMissing);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public List<T> getAllMedia (DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		try {
			return local_getAllMedia(sort, direction, hideMissing);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	public List<T> simpleSearch (String term, String esc, int maxResults) throws DbException {
		try {
			return local_simpleSearch(term, esc, maxResults);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	DB writers.
	
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
	
	public void setDateAdded (String sfile, Date date) throws DbException {
		try {
			local_setDateAdded(sfile, date);
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
	
	public static class DbColumn {
		
		private String name;
		private String defaultValue;
		private String sqlType;
		
		public DbColumn (String name, String sqlType, String defaultValue) {
			this.setName(name);
			this.setSqlType(sqlType);
			this.setDefaultValue(defaultValue);
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
		
		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}
		
		public String getDefaultValue() {
			return this.defaultValue;
		}

		public void setSqlType(String sqlType) {
			this.sqlType = sqlType;
		}

		public String getSqlType() {
			return this.sqlType;
		}
		
	}
	
	/* - - - - - - - - - - - - - - - -
	 * tbl_mediafiles
	 */
	
	private static final String SQL_TBL_MEDIAFILES_EXISTS = 
		"SELECT name FROM sqlite_master WHERE name='tbl_mediafiles';";
	
	private static final String SQL_TBL_MEDIAFILES_COL_ROWID = "ROWID"; // sqlite automatically creates this.
	private static final String SQL_TBL_MEDIAFILES_COL_FILE = "sfile";
	private static final String SQL_TBL_MEDIAFILES_COL_DADDED = "dadded";
	private static final String SQL_TBL_MEDIAFILES_COL_HASHCODE = "lmd5";
	private static final String SQL_TBL_MEDIAFILES_COL_DMODIFIED = "dmodified";
	private static final String SQL_TBL_MEDIAFILES_COL_ENABLED = "benabled";
	private static final String SQL_TBL_MEDIAFILES_COL_MISSING = "bmissing";
	private static final String SQL_TBL_MEDIAFILES_COL_REMLOC = "sremloc";
	
	protected List<DbColumn> generateSqlTblMediaFilesFields () {
		List<DbColumn> l = new LinkedList<DbColumn>();
		
		l.add(new DbColumn("sfile",     "VARCHAR(1000) not null collate nocase primary key", ""));
		l.add(new DbColumn("lmd5",      "BIGINT",   ""));
		l.add(new DbColumn("dadded",    "DATETIME", ""));
		l.add(new DbColumn("dmodified", "DATETIME", ""));
		l.add(new DbColumn("benabled",  "INT(1)",   ""));
		l.add(new DbColumn("bmissing",  "INT(1)",   ""));
		l.add(new DbColumn("sremloc",  "VARCHAR(1000) NOT NULL", ""));
		
		return l;
	}
	
	private List<DbColumn> tblMediaFilesFields;
	
	synchronized List<DbColumn> getSqlTblMediaFilesFields () {
		if (this.tblMediaFilesFields == null) {
			this.tblMediaFilesFields = generateSqlTblMediaFilesFields();
		}
		return this.tblMediaFilesFields;
	}
	
	private String getSqlTblMediaFilesCreate () {
		StringBuilder sb = new StringBuilder();
		List<DbColumn> ef = getSqlTblMediaFilesFields();
		
		sb.append("create table tbl_mediafiles(");
		for (DbColumn c : ef) {
			sb.append(c.getName());
			sb.append(" ");
			sb.append(c.getSqlType());
			sb.append(",");
		}
		sb.append(");");
		
		return sb.toString();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Library queries.
	
	GeneratedString sqlTblMediaFilesFieldNames = new GeneratedString () {
		@Override
		public String generateString() {
			StringBuilder sb = new StringBuilder();
    		List<DbColumn> ef = getSqlTblMediaFilesFields();
    		
    		boolean first = true;
    		for (DbColumn c : ef) {
    			if (first) {
    				first = false;
    			} else {
    				sb.append(",");
    			}
    			sb.append(c.getName());
    		}
    		
			return sb.toString();
		}
	};
	
	private GeneratedString sqlTblMEdiaFiles_qAll = new GeneratedString () {
		@Override
		public String generateString() {
			return 
				"SELECT ROWID, " + MediaSqliteLayer2.this.sqlTblMediaFilesFieldNames.toString() +
				" FROM tbl_mediafiles" +
				" ORDER BY {COL} {DIR};";
		};
	};
	
	private GeneratedString sqlTblMEdiaFiles_qNotMissing = new GeneratedString () {
		@Override
		public String generateString() {
			return 
			"SELECT ROWID, " + MediaSqliteLayer2.this.sqlTblMediaFilesFieldNames.toString() +
			" FROM tbl_mediafiles" +
			" WHERE (bmissing<>1 OR bmissing is NULL)" +
			" ORDER BY {COL} {DIR};";
		};
	};
	
	private GeneratedString sqlTblMEdiaFiles_qSimpleSearch = new GeneratedString () {
		@Override
		public String generateString() {
			return 
			"SELECT ROWID, " + MediaSqliteLayer2.this.sqlTblMediaFilesFieldNames.toString() +
			" FROM tbl_mediafiles" +
			" WHERE sfile LIKE ? ESCAPE ?" +
		    " AND (bmissing<>1 OR bmissing is NULL) AND (benabled<>0 OR benabled is NULL)" +
		    " ORDER BY dlastplay DESC, lendcnt DESC, lstartcnt DESC, sfile COLLATE NOCASE ASC;";
		};
	};
	
	private static final String SQL_TBL_MEDIAFILES_Q_EXISTS =
		"SELECT count(*) FROM tbl_mediafiles WHERE sfile=? COLLATE NOCASE;";
	
	private String sqlTblMediaFilesAdd = null;
	
	private synchronized String getSqlTblMediaFilesAdd () {
		if (this.sqlTblMediaFilesAdd == null) {
    		StringBuilder sb = new StringBuilder();
    		List<DbColumn> ef = getSqlTblMediaFilesFields();
    		
    		sb.append("INSERT INTO tbl_mediafiles (sfile,dadded,lstartcnt,lendcnt,dmodified,lduration,benabled,bmissing,sremloc");
    		for (DbColumn c : ef) {
    			if (c.getDefaultValue() != null) {
        			sb.append(",");
        			sb.append(c.getName());
    			}
    		}
    		sb.append(" (?,?,0,0,?,-1,1,0,''");
    		for (DbColumn c : ef) {
    			if (c.getDefaultValue() != null) {
        			sb.append(",");
        			sb.append(c.getDefaultValue());
    			}
    		}
    		sb.append(");");
    		
    		this.sqlTblMediaFilesAdd = sb.toString();
		}
		
		return this.sqlTblMediaFilesAdd;
	}
	
	private static final String SQL_TBL_MEDIAFILES_REMOVE =
		"DELETE FROM tbl_mediafiles WHERE sfile=?";
	
	private static final String SQL_TBL_MEDIAFILES_REMOVE_BYROWID =
		"DELETE FROM tbl_mediafiles WHERE ROWID=?";
	
	private static final String SQL_TBL_MEDIAFILES_SETDATEADDED =
		"UPDATE tbl_mediafiles SET dadded=?" +
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
	
	public enum SortDirection { ASC, DESC };
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Superclass methods.
	
	@Override
	protected List<SqlCreateCmd> getTblCreateCmds() {
		List<SqlCreateCmd> l = super.getTblCreateCmds();
		
		l.add(new SqlCreateCmd(SQL_TBL_MEDIAFILES_EXISTS, getSqlTblMediaFilesCreate()));
		
		return l;
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media.
	
	private SimpleDateFormat SQL_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private List<T> local_updateListOfAllMedia (List<T> list, DbColumn sort, SortDirection direction, boolean hideMissing) throws SQLException, ClassNotFoundException {
		String sql = local_getAllMediaSql(sort, direction, hideMissing);
		ResultSet rs;
		
		List<T> ret;
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
	
	private List<T> local_getAllMedia (DbColumn sort, SortDirection direction, boolean hideMissing) throws SQLException, ClassNotFoundException {
		String sql = local_getAllMediaSql(sort, direction, hideMissing);
		ResultSet rs;
		
		List<T> ret;
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
	
	private String local_getAllMediaSql (DbColumn sort, SortDirection direction, boolean hideMissing) {
		String sql;
		
		if (hideMissing) {
			sql = this.sqlTblMEdiaFiles_qNotMissing.toString();
		} else {
			sql = this.sqlTblMEdiaFiles_qAll.toString();
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
		
		sql = sql.replace("{COL}", sort.getName()); // FIXME make file sort non-case sensitive?
		
		return sql;
	}
	
	private List<T> local_simpleSearch (String term, String esc, int maxResults) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		List<T> ret;
		
		ps = getDbCon().prepareStatement(this.sqlTblMEdiaFiles_qSimpleSearch.toString());
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
	
	private List<T> local_parseAndUpdateFromRecordSet (List<T> list, ResultSet rs) throws SQLException {
		List<T> finalList = new ArrayList<T>();
		
		// Build a HashMap of existing items to make lookup a lot faster.
		Map<String, T> keepMap = new HashMap<String, T>(list.size());
		for (T e : list) {
			keepMap.put(e.getFilepath(), e);
		}
		
		/* Extract entry from DB.  Compare it to existing entries and
		 * create new list as we go. 
		 */
		while (rs.next()) {
			T newItem = getNewT();
			newItem.setFilepath(rs.getString(SQL_TBL_MEDIAFILES_COL_FILE));
			newItem.setDateAdded(readDate(rs, SQL_TBL_MEDIAFILES_COL_DADDED));
			newItem.setHashcode(rs.getLong(SQL_TBL_MEDIAFILES_COL_HASHCODE));
			newItem.setDateLastModified(readDate(rs, SQL_TBL_MEDIAFILES_COL_DMODIFIED));
			newItem.setEnabled(rs.getInt(SQL_TBL_MEDIAFILES_COL_ENABLED) != 0); // default to true.
			newItem.setMissing(rs.getInt(SQL_TBL_MEDIAFILES_COL_MISSING) == 1); // default to false.
			newItem.setDbRowId(rs.getLong(SQL_TBL_MEDIAFILES_COL_ROWID));
			newItem.setRemoteLocation(rs.getString(SQL_TBL_MEDIAFILES_COL_REMLOC));
			
			setTFromRs(newItem, rs);
			
			T oldItem = keepMap.get(newItem.getFilepath());
			if (oldItem != null) {
				oldItem.setFromMediaItem(newItem);
				finalList.add(oldItem);
			} else {
				finalList.add(newItem);
			}
		}
		
		return finalList;
	}
	
	private List<T> local_parseRecordSet (ResultSet rs) throws SQLException {
		List<T> ret = new ArrayList<T>();
		
		while (rs.next()) {
			T mt = getNewT();
			mt.setFilepath(rs.getString(SQL_TBL_MEDIAFILES_COL_FILE));
			mt.setDateAdded(readDate(rs, SQL_TBL_MEDIAFILES_COL_DADDED));
			mt.setHashcode(rs.getLong(SQL_TBL_MEDIAFILES_COL_HASHCODE));
			mt.setDateLastModified(readDate(rs, SQL_TBL_MEDIAFILES_COL_DMODIFIED));
			mt.setEnabled(rs.getInt(SQL_TBL_MEDIAFILES_COL_ENABLED) != 0); // default to true.
			mt.setMissing(rs.getInt(SQL_TBL_MEDIAFILES_COL_MISSING) == 1); // default to false.
			mt.setDbRowId(rs.getLong(SQL_TBL_MEDIAFILES_COL_ROWID));
			mt.setRemoteLocation(rs.getString(SQL_TBL_MEDIAFILES_COL_REMLOC));
			
			setTFromRs(mt, rs);
			
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
			ps = getDbCon().prepareStatement(getSqlTblMediaFilesAdd());
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
	
	/**
	 * This method will read the date from the DB
	 * weather it was stored as a number or a string.
	 * Morrigan uses the number method, but terra used strings.
	 * Using this method allows for backward compatability.
	 */
	protected Date readDate (ResultSet rs, String column) throws SQLException {
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
