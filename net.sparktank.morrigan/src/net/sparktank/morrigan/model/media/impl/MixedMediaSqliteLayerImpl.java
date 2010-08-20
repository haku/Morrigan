package net.sparktank.morrigan.model.media.impl;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sparktank.morrigan.helpers.GeneratedString;
import net.sparktank.morrigan.model.MediaSqliteLayer;
import net.sparktank.morrigan.model.db.SqliteHelper;
import net.sparktank.morrigan.model.db.impl.DbColumn;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.morrigan.model.db.interfaces.IDbItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaPicture;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem.MediaType;
import net.sparktank.sqlitewrapper.DbException;

public abstract class MixedMediaSqliteLayerImpl extends MediaSqliteLayer<IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected MixedMediaSqliteLayerImpl (String dbFilePath, boolean autoCommit) throws DbException {
		super(dbFilePath, autoCommit);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	SQL for defining tables.
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	SQL for defining tbl_mediafiles.
	
	public static final String SQL_TBL_MEDIAFILES_NAME = "tbl_mediafiles";
	
	public static final IDbColumn SQL_TBL_MEDIAFILES_COL_ROWID     = new DbColumn("ROWID", null, null, null);
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_TYPE      = new DbColumn("type",       "type",          "INT",      "?");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_FILE      = new DbColumn("sfile",      "file path",     "VARCHAR(1000) not null collate nocase primary key", "?", " collate nocase");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_HASHCODE  = new DbColumn("lmd5",       "hashcode",      "BIGINT",   null);
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_DADDED    = new DbColumn("dadded",     "date added",    "DATETIME", "?");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_DMODIFIED = new DbColumn("dmodified",  "date modified", "DATETIME", "?");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_ENABLED   = new DbColumn("benabled",   null,            "INT(1)",   "1");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_MISSING   = new DbColumn("bmissing",   null,            "INT(1)",   "0");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_REMLOC    = new DbColumn("sremloc",    null,            "VARCHAR(1000) NOT NULL", "''");
	
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_STARTCNT =  new DbColumn("lstartcnt", "start count", "INT(6)",   "0");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_ENDCNT =    new DbColumn("lendcnt",   "end count",   "INT(6)",   "0");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_DLASTPLAY = new DbColumn("dlastplay", "last played", "DATETIME", null);
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_DURATION =  new DbColumn("lduration", "duration",    "INT(6)",   "-1");
	
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_WIDTH =  new DbColumn("lwidth",  "width",  "INT(6)",   "0");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_HEIGHT = new DbColumn("lheight", "height", "INT(6)",   "0");
	
	public static final DbColumn[] SQL_TBL_MEDIAFILES_COLS = new DbColumn[] {
		SQL_TBL_MEDIAFILES_COL_TYPE,
		SQL_TBL_MEDIAFILES_COL_FILE,
		SQL_TBL_MEDIAFILES_COL_HASHCODE,
		SQL_TBL_MEDIAFILES_COL_DADDED,
		SQL_TBL_MEDIAFILES_COL_DMODIFIED,
		SQL_TBL_MEDIAFILES_COL_ENABLED,
		SQL_TBL_MEDIAFILES_COL_MISSING,
		SQL_TBL_MEDIAFILES_COL_REMLOC,
		
		SQL_TBL_MEDIAFILES_COL_STARTCNT,
		SQL_TBL_MEDIAFILES_COL_ENDCNT,
		SQL_TBL_MEDIAFILES_COL_DLASTPLAY,
		SQL_TBL_MEDIAFILES_COL_DURATION,
		
		SQL_TBL_MEDIAFILES_COL_WIDTH,
		SQL_TBL_MEDIAFILES_COL_HEIGHT,
		};
	
	static public IDbColumn parseColumnFromName (String name) {
		for (IDbColumn c : SQL_TBL_MEDIAFILES_COLS) {
			if (c.getName().equals(name)) {
				return c;
			}
		}
		throw new IllegalArgumentException();
	}
	
	static protected List<DbColumn> generateSqlTblMediaFilesColumns () {
		List<DbColumn> l = new LinkedList<DbColumn>();
		for (DbColumn c : SQL_TBL_MEDIAFILES_COLS) {
			l.add(c);
		}
		return l;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	SQL queries.
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	Fragments.
	
	private static final String _SQL_MEDIAFILES_SELECT =
		"SELECT"
		+ " ROWID, type, sfile, lmd5, dadded, dmodified, benabled, bmissing, sremloc"
		+ ",lstartcnt,lendcnt,dlastplay,lduration"
    	+ ",lwidth,lheight"
    	+ " FROM tbl_mediafiles";
	
	private static final String _SQL_WHERE =
		" WHERE";
	
	private static final String _SQL_AND =
		" AND";
	
	private static final String _SQL_MEDIAFILES_WHERTYPE =
		" type=?";
	
	private static final String _SQL_MEDIAFILES_WHERENOTMISSING =
		" (bmissing<>1 OR bmissing is NULL)";
	
	private static final String _SQL_ORDERBYREPLACE =
		" ORDER BY {COL} {DIR};";
	
	private static final String _SQL_MEDIAFILES_WHEREORDERSEARCH = 
		" sfile LIKE ? ESCAPE ?"
		+ " AND (bmissing<>1 OR bmissing is NULL) AND (benabled<>0 OR benabled is NULL)"
		+ " ORDER BY dlastplay DESC, lendcnt DESC, lstartcnt DESC, sfile COLLATE NOCASE ASC;";
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	MediaMixedItem Queries.
	
	private static final String SQL_MEDIAFILES_Q_ALL =
		_SQL_MEDIAFILES_SELECT
		+ _SQL_ORDERBYREPLACE;
	
	private static final String SQL_MEDIAFILES_Q_NOTMISSING =
		_SQL_MEDIAFILES_SELECT
    	+ _SQL_WHERE + _SQL_MEDIAFILES_WHERENOTMISSING
    	+ _SQL_ORDERBYREPLACE;
	
	private static final String SQL_MEDIAFILES_Q_SIMPLESEARCH =
		_SQL_MEDIAFILES_SELECT
		+ _SQL_WHERE + _SQL_MEDIAFILES_WHEREORDERSEARCH;
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	MediaMixedItem Queries - typed.
	
	private static final String SQL_MEDIAFILES_Q_ALL_T =
		_SQL_MEDIAFILES_SELECT
		+ _SQL_WHERE + _SQL_MEDIAFILES_WHERTYPE
		+ _SQL_ORDERBYREPLACE;
	
	private static final String SQL_MEDIAFILES_Q_NOTMISSING_T =
		_SQL_MEDIAFILES_SELECT
		+ _SQL_WHERE + _SQL_MEDIAFILES_WHERTYPE + _SQL_AND + _SQL_MEDIAFILES_WHERENOTMISSING
		+ _SQL_ORDERBYREPLACE;
	
	private static final String SQL_MEDIAFILES_Q_SIMPLESEARCH_T =
		_SQL_MEDIAFILES_SELECT
		+ _SQL_WHERE + _SQL_MEDIAFILES_WHERTYPE + _SQL_AND + _SQL_MEDIAFILES_WHEREORDERSEARCH;
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	Adding and removing tracks.
	
	private static final String SQL_TBL_MEDIAFILES_Q_EXISTS =
		"SELECT count(*) FROM tbl_mediafiles WHERE sfile=? COLLATE NOCASE;";
	
//	TODO move to helper / where DbColumn is defined?
	private GeneratedString sqlTblMediaFilesAdd = new GeneratedString() {
		@Override
		public String generateString() {
			StringBuilder sb = new StringBuilder();
    		IDbColumn[] cols = SQL_TBL_MEDIAFILES_COLS;
    		
    		sb.append("INSERT INTO tbl_mediafiles (");
    		boolean first = true;
    		for (IDbColumn c : cols) {
    			if (c.getDefaultValue() != null) {
    				if (first) {
        				first = false;
        			} else {
        				sb.append(",");
        			}
        			sb.append(c.getName());
    			}
    		}
    		sb.append(") VALUES (");
    		first = true;
    		for (IDbColumn c : cols) {
    			if (c.getDefaultValue() != null) {
    				if (first) {
        				first = false;
        			} else {
        				sb.append(",");
        			}
        			sb.append(c.getDefaultValue());
    			}
    		}
    		sb.append(");");
    		
    		return sb.toString();
		};
	};
	
	private static final String SQL_TBL_MEDIAFILES_REMOVE =
		"DELETE FROM tbl_mediafiles WHERE sfile=?";
	
	private static final String SQL_TBL_MEDIAFILES_REMOVE_BYROWID =
		"DELETE FROM tbl_mediafiles WHERE ROWID=?";
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	Setting MediaItem data.
	
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
	
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	Setting MixedMediaItem data.
	
	private static final String SQL_TBL_MEDIAFILES_SETTYPE =
		"UPDATE tbl_mediafiles SET type=?" +
		" WHERE sfile=?;";
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	Setting MediaTrack data.
	
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
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	Setting MediaPicture data.
	
	private static final String SQL_TBL_MEDIAFILES_SETDIMENSIONS =
		"UPDATE tbl_mediafiles SET lwidth=?,lheight=?" +
		" WHERE sfile=?;";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Create statements.
	
	@Override
	protected List<SqlCreateCmd> getTblCreateCmds() {
		List<SqlCreateCmd> l = super.getTblCreateCmds();
		
		l.add(SqliteHelper.generateSql_Create(SQL_TBL_MEDIAFILES_NAME, SQL_TBL_MEDIAFILES_COLS));
		
		return l;
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem getters.
	
	protected List<IMixedMediaItem> local_getAllMedia (MediaType mediaType, IDbColumn sort, SortDirection direction, boolean hideMissing) throws SQLException, ClassNotFoundException {
		String sql = local_getAllMediaSql(SQL_MEDIAFILES_Q_ALL, SQL_MEDIAFILES_Q_NOTMISSING, SQL_MEDIAFILES_Q_ALL_T, SQL_MEDIAFILES_Q_NOTMISSING_T, mediaType, hideMissing, sort, direction);
		ResultSet rs;
		
		List<IMixedMediaItem> ret;
		PreparedStatement ps = getDbCon().prepareStatement(sql);
		try {
			if (mediaType != MediaType.UNKNOWN) {
				ps.setInt(1, mediaType.getN());
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
	
	protected List<IMixedMediaItem> local_updateListOfAllMedia (MediaType mediaType, List<IMixedMediaItem> list, IDbColumn sort, SortDirection direction, boolean hideMissing) throws SQLException, ClassNotFoundException {
		String sql = local_getAllMediaSql(SQL_MEDIAFILES_Q_ALL, SQL_MEDIAFILES_Q_NOTMISSING, SQL_MEDIAFILES_Q_ALL_T, SQL_MEDIAFILES_Q_NOTMISSING_T, mediaType, hideMissing, sort, direction);
		ResultSet rs;
		
		List<IMixedMediaItem> ret;
		PreparedStatement ps = getDbCon().prepareStatement(sql);
		try {
			if (mediaType != MediaType.UNKNOWN) {
				ps.setInt(1, mediaType.getN());
			}
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
	
	protected List<IMixedMediaItem> local_simpleSearch (MediaType mediaType, String term, String esc, int maxResults) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		List<IMixedMediaItem> ret;
		
		String sql;
		if (mediaType == MediaType.UNKNOWN) {
			sql = SQL_MEDIAFILES_Q_SIMPLESEARCH;
		} else {
			sql = SQL_MEDIAFILES_Q_SIMPLESEARCH_T;
		}
		
		ps = getDbCon().prepareStatement(sql);
		
		try {
			if (mediaType == MediaType.UNKNOWN) {
				ps.setString(1, "%" + term + "%");
				ps.setString(2, esc);
			}
			else {
				ps.setInt(1, mediaType.getN());
				ps.setString(2, "%" + term + "%");
				ps.setString(3, esc);
			}
			
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media queries.
	
	protected boolean local_hasFile (String filePath) throws SQLException, ClassNotFoundException {
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
		
		return (n > 0);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media adders and removers.
	
	protected boolean local_addTrack (MediaType mediaType, String filePath, long lastModified) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		
		int n;
		if (!local_hasFile(filePath)) {
			System.err.println("Adding file '" + filePath + "' to '"+getDbFilePath()+"'.");
			ps = getDbCon().prepareStatement(this.sqlTblMediaFilesAdd.toString());
			try {
				ps.setInt(1, mediaType.getN());
				ps.setString(2, filePath);
				ps.setDate(3, new java.sql.Date(new Date().getTime()));
				ps.setDate(4, new java.sql.Date(lastModified));
				n = ps.executeUpdate();
			} finally {
				ps.close();
			}
			if (n<1) throw new DbException("No update occured for addTrack('"+filePath+"','"+lastModified+"').");
			
			return true;
		}
		
		return false;
	}
	
	protected boolean[] local_addFiles (List<File> files) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		int[] n;
		
		ps = getDbCon().prepareStatement(this.sqlTblMediaFilesAdd.toString());
		try {
			for (File file : files) {
				String filePath = file.getAbsolutePath();
				if (!local_hasFile(filePath)) {
					ps.setInt(1, MediaType.UNKNOWN.getN());
					ps.setString(2, filePath);
					ps.setDate(3, new java.sql.Date(new Date().getTime()));
					ps.setDate(4, new java.sql.Date(file.lastModified()));
					ps.addBatch();
				}
			}
			
			n = ps.executeBatch();
			
			boolean[] b = new boolean[n.length];
			for (int i = 0; i < n.length; i++) {
				b[i] = (n[i] > 0 || n[i] == Statement.SUCCESS_NO_INFO);
			}
			return b;
		}
		finally {
			ps.close();
		}
	}
	
	protected int local_removeTrack (String sfile) throws SQLException, ClassNotFoundException {
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
	
	protected int local_removeTrack (IDbItem dbItem) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		int ret;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_REMOVE_BYROWID);
		try {
			ps.setLong(1, dbItem.getDbRowId());
			ret = ps.executeUpdate();
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem setters.
	
	protected void local_setDateAdded (String sfile, Date date) throws Exception, ClassNotFoundException {
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
		if (n<1) throw new DbException("No update occured for local_setDateAdded('"+sfile+"','"+date+"').");
	}
	
	protected void local_setHashCode (String sfile, long hashcode) throws SQLException, ClassNotFoundException, DbException {
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
		if (n<1) throw new DbException("No update occured for local_setHashCode('"+sfile+"','"+hashcode+"').");
	}
	
	protected void local_setDateLastModified (String sfile, Date date) throws SQLException, ClassNotFoundException, DbException {
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
		if (n<1) throw new DbException("No update occured for local_setDateLastModified('"+sfile+"','"+date+"').");
	}
	
	protected void local_setEnabled (String sfile, boolean value) throws SQLException, ClassNotFoundException, DbException {
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
		if (n<1) throw new DbException("No update occured for local_setEnabled('"+sfile+"','"+value+"').");
	}
	
	protected void local_setMissing (String sfile, boolean value) throws SQLException, ClassNotFoundException, DbException {
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
		if (n<1) throw new DbException("No update occured for local_setMissing('"+sfile+"','"+value+"').");
	}
	
	protected void local_setRemoteLocation(String sfile, String remoteLocation) throws SQLException, ClassNotFoundException, DbException {
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
		if (n<1) throw new DbException("No update occured for local_setRemoteLocation('"+sfile+"','"+remoteLocation+"').");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MixedMediaItem setters.
	
	protected void local_setItemMediaType(String sfile, MediaType newType) throws DbException, SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETTYPE);
		int n;
		try {
			ps.setInt(1, newType.getN());
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaTrack setters.
	
	protected void local_incTrackStartCnt (String sfile, long x) throws SQLException, ClassNotFoundException, DbException {
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
	
	protected void local_setTrackStartCnt (String sfile, long x) throws SQLException, ClassNotFoundException, DbException {
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
	
	protected void local_setDateLastPlayed (String sfile, Date date) throws SQLException, ClassNotFoundException, DbException {
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
	
	protected void local_incTrackEndCnt (String sfile, long x) throws SQLException, ClassNotFoundException, DbException {
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
	
	protected void local_setTrackEndCnt (String sfile, long x) throws SQLException, ClassNotFoundException, DbException {
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
	
	protected void local_setTrackDuration (String sfile, int duration) throws SQLException, ClassNotFoundException, DbException {
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
//	MediaPic setters.
	
	protected void local_setDimensions(String sfile, int width, int height) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDIMENSIONS);
		int n;
		try {
			ps.setInt(1, width);
			ps.setInt(2, height);
			ps.setString(3, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for setRemoteLocation('"+sfile+"','"+width+"','"+height+"').");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
//	=== === === === === === === === === === === === === === === === === === ===
//	===
//	===     Static methods.
//	===
//	=== === === === === === === === === === === === === === === === === === ===
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem getters.
	
	static private String local_getAllMediaSql (
			String sqlAll, String sqlNotMissing, String sqlAllT, String sqlNotMissingT,
			MediaType mediaType, boolean hideMissing, IDbColumn sort, SortDirection direction) {
		
		String sql;
		
		if (mediaType == MediaType.UNKNOWN) {
			if (hideMissing) {
				sql = sqlNotMissing;
			} else {
				sql = sqlAll;
			}
		}
		else {
			if (hideMissing) {
				sql = sqlNotMissingT;
			} else {
				sql = sqlAllT;
			}
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
		
		String sortTerm = sort.getName();
		if (sort.getSortOpts() != null) {
			sortTerm = sortTerm.concat(sort.getSortOpts());
		}
		sql = sql.replace("{COL}", sortTerm);
		
		return sql;
	}
	
	static private List<IMixedMediaItem> local_parseAndUpdateFromRecordSet (List<IMixedMediaItem> list, ResultSet rs) throws SQLException {
		List<IMixedMediaItem> finalList = new ArrayList<IMixedMediaItem>();
		
		// Build a HashMap of existing items to make lookup a lot faster.
		Map<String, IMixedMediaItem> keepMap = new HashMap<String, IMixedMediaItem>(list.size());
		for (IMixedMediaItem e : list) {
			keepMap.put(e.getFilepath(), e);
		}
		
		/* Extract entry from DB.  Compare it to existing entries and
		 * create new list as we go. 
		 */
		while (rs.next()) {
			IMixedMediaItem newItem = createMediaItem(rs);
			IMixedMediaItem oldItem = keepMap.get(newItem.getFilepath());
			if (oldItem != null) {
				oldItem.setFromMediaItem(newItem);
				finalList.add(oldItem);
			} else {
				finalList.add(newItem);
			}
		}
		
		return finalList;
	}
	
	static private List<IMixedMediaItem> local_parseRecordSet (ResultSet rs) throws SQLException {
		List<IMixedMediaItem> ret = new ArrayList<IMixedMediaItem>();
		
		while (rs.next()) {
			IMixedMediaItem mi = createMediaItem(rs);
			ret.add(mi);
		}
		
		return ret;
	}
	
	static protected IMixedMediaItem createMediaItem (ResultSet rs) throws SQLException {
		int i = rs.getInt(SQL_TBL_MEDIAFILES_COL_TYPE.getName());
		MediaType t = MediaType.parseInt(i);
		IMixedMediaItem mi = new MixedMediaItem(t);
		
		switch (t) {
			case TRACK:
				readMediaTrack(rs, mi);
				break;
			
			case PICTURE:
				readMediaPic(rs, mi);
				break;
			
			case UNKNOWN:
				readMediaItem(rs, mi);
			
		}
		
		return mi;
	}
	
	static protected void readMediaTrack (ResultSet rs, IMediaTrack mi) throws SQLException {
		readMediaItem(rs, mi);
		
		mi.setStartCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_STARTCNT.getName()));
		mi.setEndCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_ENDCNT.getName()));
		mi.setDuration(rs.getInt(SQL_TBL_MEDIAFILES_COL_DURATION.getName()));
		mi.setDateLastPlayed(SqliteHelper.readDate(rs, SQL_TBL_MEDIAFILES_COL_DLASTPLAY.getName()));
	}
	
	static protected void readMediaPic (ResultSet rs, IMediaPicture mi) throws SQLException {
		readMediaItem(rs, mi);
		
		mi.setWidth(rs.getInt(SQL_TBL_MEDIAFILES_COL_WIDTH.getName()));
		mi.setHeight(rs.getInt(SQL_TBL_MEDIAFILES_COL_HEIGHT.getName()));
	}
	
	static protected void readMediaItem (ResultSet rs, IMediaItem mi) throws SQLException {
		mi.setFilepath(rs.getString(SQL_TBL_MEDIAFILES_COL_FILE.getName()));
		mi.setDateAdded(SqliteHelper.readDate(rs, SQL_TBL_MEDIAFILES_COL_DADDED.getName()));
		mi.setHashcode(rs.getLong(SQL_TBL_MEDIAFILES_COL_HASHCODE.getName()));
		mi.setDateLastModified(SqliteHelper.readDate(rs, SQL_TBL_MEDIAFILES_COL_DMODIFIED.getName()));
		mi.setEnabled(rs.getInt(SQL_TBL_MEDIAFILES_COL_ENABLED.getName()) != 0); // default to true.
		mi.setMissing(rs.getInt(SQL_TBL_MEDIAFILES_COL_MISSING.getName()) == 1); // default to false.
		mi.setDbRowId(rs.getLong(SQL_TBL_MEDIAFILES_COL_ROWID.getName()));
		mi.setRemoteLocation(rs.getString(SQL_TBL_MEDIAFILES_COL_REMLOC.getName()));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
