package net.sparktank.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.model.db.IDbColumn;
import net.sparktank.morrigan.model.db.IDbItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.media.IMixedMediaItemStorageLayer;
import net.sparktank.morrigan.model.media.internal.db.MediaSqliteLayer;
import net.sparktank.morrigan.model.media.internal.db.SqliteHelper;
import net.sparktank.morrigan.util.GeneratedString;
import net.sparktank.sqlitewrapper.DbException;

public abstract class MixedMediaSqliteLayerInner extends MediaSqliteLayer<IMixedMediaItem> implements IMixedMediaItemStorageLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MixedMediaItemFactory itemFactory;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected MixedMediaSqliteLayerInner (String dbFilePath, boolean autoCommit, MixedMediaItemFactory itemFactory) throws DbException {
		super(dbFilePath, autoCommit);
		this.itemFactory = itemFactory;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	SQL for defining tables.
	
//	-  -  -  -  -  -  -  -  -  -  -  -
//	SQL for defining tbl_mediafiles.
	
	public static final String SQL_TBL_MEDIAFILES_NAME = "tbl_mediafiles";
	
	public static final IDbColumn[] SQL_TBL_MEDIAFILES_COLS = new IDbColumn[] {
		SQL_TBL_MEDIAFILES_COL_TYPE,
		SQL_TBL_MEDIAFILES_COL_FILE,
		SQL_TBL_MEDIAFILES_COL_MD5,
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
	
	static protected List<IDbColumn> generateSqlTblMediaFilesColumns () {
		List<IDbColumn> l = new LinkedList<IDbColumn>();
		for (IDbColumn c : SQL_TBL_MEDIAFILES_COLS) {
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
		+ " ROWID, type, sfile, md5, dadded, dmodified, benabled, bmissing, sremloc"
		+ ",lstartcnt,lendcnt,dlastplay,lduration"
    	+ ",lwidth,lheight"
    	+ " FROM tbl_mediafiles";
	
	private static final String _SQL_MEDIAFILESTAGS_SELECT =
		"SELECT"
		+ " distinct m.ROWID AS ROWID,m.type AS type,sfile,md5,dadded,dmodified,benabled,bmissing,sremloc"
		+ ",lstartcnt,lendcnt,dlastplay,lduration"
		+ ",lwidth,lheight"
		+ " FROM tbl_mediafiles AS m LEFT OUTER JOIN tbl_tags ON m.ROWID=tbl_tags.mf_rowid";
	
	private static final String _SQL_WHERE =
		" WHERE";
	
	private static final String _SQL_AND =
		" AND";
	
	private static final String _SQL_MEDIAFILES_WHERTYPE =
		" type=?";
	
	private static final String _SQL_MEDIAFILESTAGS_WHERTYPE =
		" m.type=?";
	
	private static final String _SQL_MEDIAFILES_WHERENOTMISSING =
		" (bmissing<>1 OR bmissing is NULL)";
	
	private static final String _SQL_ORDERBYREPLACE =
		" ORDER BY {COL} {DIR};";
	
	private static final String _SQL_MEDIAFILES_WHEREFILEEQ = 
		" sfile = ?";
	
	private static final String _SQL_MEDIAFILES_WHERESEARCHTAGS = 
		" (sfile LIKE ? ESCAPE ? OR tag LIKE ? ESCAPE ?)";
	
//	private static final String _SQL_MEDIAFILES_WHEREORDERSEARCH = 
//		" sfile LIKE ? ESCAPE ?"
//		+ " AND (bmissing<>1 OR bmissing is NULL) AND (benabled<>0 OR benabled is NULL)"
//		+ " ORDER BY dlastplay DESC, lendcnt DESC, lstartcnt DESC, sfile COLLATE NOCASE ASC;";
	
	private static final String _SQL_MEDIAFILESTAGS_WHEREORDERSEARCH = 
		" sfile LIKE ? ESCAPE ? OR tag LIKE ? ESCAPE ?"
		+ " AND (bmissing<>1 OR bmissing is NULL) AND (benabled<>0 OR benabled is NULL)"
		+ " ORDER BY dlastplay DESC, lendcnt DESC, lstartcnt DESC, sfile COLLATE NOCASE ASC;";
	
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
		}
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
		"UPDATE tbl_mediafiles SET md5=?" +
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
	
	private static final String SQL_TBL_MEDIAFILES_TRACKPLAYED =
		"UPDATE tbl_mediafiles SET lstartcnt=lstartcnt+?,dlastplay=?" +
		" WHERE sfile=?;";
	
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
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem getters.
	
	protected List<IMixedMediaItem> local_getAllMedia (MediaType mediaType, IDbColumn sort, SortDirection direction, boolean hideMissing) throws SQLException, ClassNotFoundException {
		String sql = local_getAllMediaSql(mediaType, hideMissing, sort, direction, null);
		ResultSet rs;
		
		List<IMixedMediaItem> ret;
		PreparedStatement ps = getDbCon().prepareStatement(sql);
		try {
			if (mediaType != MediaType.UNKNOWN) {
				ps.setInt(1, mediaType.getN());
			}
			rs = ps.executeQuery();
			try {
				ret = local_parseRecordSet(rs, this.itemFactory);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
	protected List<IMixedMediaItem> local_getAllMedia (MediaType mediaType, IDbColumn sort, SortDirection direction, boolean hideMissing, String search, String searchEsc) throws SQLException, ClassNotFoundException {
		String sql = local_getAllMediaSql(mediaType, hideMissing, sort, direction, search);
		ResultSet rs;
		
		List<IMixedMediaItem> ret;
		PreparedStatement ps = getDbCon().prepareStatement(sql);
		int n = 1;
		try {
			if (mediaType != MediaType.UNKNOWN) {
				ps.setInt(n, mediaType.getN());
				n++;
			}
			if (search != null) {
				ps.setString(n, "%" + search + "%");
				n++;
				ps.setString(n, searchEsc);
				n++;
				ps.setString(n, "%" + search + "%");
				n++;
				ps.setString(n, searchEsc);
				n++;
			}
			rs = ps.executeQuery();
			try {
				ret = local_parseRecordSet(rs, this.itemFactory);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
		
		return ret;
	}
	
	/**
	 * Querying for type UNKNOWN will return all types (i.e. wild-card).
	 */
	protected List<IMixedMediaItem> local_simpleSearch (MediaType mediaType, String term, String esc, int maxResults) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		List<IMixedMediaItem> ret;
		
		String sql;
		if (mediaType == MediaType.UNKNOWN) {
			sql = _SQL_MEDIAFILESTAGS_SELECT
				+ _SQL_WHERE + _SQL_MEDIAFILESTAGS_WHEREORDERSEARCH;
		} else {
			sql = _SQL_MEDIAFILESTAGS_SELECT
				+ _SQL_WHERE + _SQL_MEDIAFILESTAGS_WHERTYPE + _SQL_AND + _SQL_MEDIAFILESTAGS_WHEREORDERSEARCH;
		}
		
		try {
			ps = getDbCon().prepareStatement(sql);
		}
		catch (SQLException e) {
			System.err.println("sql='"+sql+"'");
			throw e;
		}
		
		try {
			if (mediaType == MediaType.UNKNOWN) {
				ps.setString(1, "%" + term + "%");
				ps.setString(2, esc);
				ps.setString(3, "%" + term + "%");
				ps.setString(4, esc);
			}
			else {
				ps.setInt(1, mediaType.getN());
				ps.setString(2, "%" + term + "%");
				ps.setString(3, esc);
				ps.setString(4, "%" + term + "%");
				ps.setString(5, esc);
			}
			
			if (maxResults > 0) {
				ps.setMaxRows(maxResults);
			}
			
			rs = ps.executeQuery();
			try {
				ret = local_parseRecordSet(rs, this.itemFactory);
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
	
	protected IMixedMediaItem local_getByFile (String filePath) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		List<IMixedMediaItem> res;
		
		String sql = _SQL_MEDIAFILES_SELECT + _SQL_WHERE + _SQL_MEDIAFILES_WHEREFILEEQ;
		ps = getDbCon().prepareStatement(sql);
		try {
			ps.setString(1, filePath);
			ps.setMaxRows(2); // Ask for 1, so we know if there is more than 1.
			
			rs = ps.executeQuery();
			try {
				res = local_parseRecordSet(rs, this.itemFactory);
			} finally {
				rs.close();
			}
		} finally {
			ps.close();
		}
		
		if (res.size() == 1) return res.get(0);
		throw new IllegalArgumentException("File not found '"+filePath+"' (results count = "+res.size()+").");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media adders and removers.
	
	protected boolean local_addTrack (MediaType mediaType, String filePath, long lastModified) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		
		int n;
		if (!local_hasFile(filePath)) {
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
			
			getChangeCaller().mediaItemAdded(filePath);
			
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
			final List<File> added = new LinkedList<File>();
			for (int i = 0; i < n.length; i++) {
				b[i] = (n[i] > 0 || n[i] == Statement.SUCCESS_NO_INFO);
				if (b[i]) added.add(files.get(i));
			}
			getChangeCaller().mediaItemsAdded(added);
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
		
		if (ret == 1) getChangeCaller().mediaItemRemoved(sfile);
		
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
		
		if (ret == 1) getChangeCaller().mediaItemRemoved(null); // FIXME pass a useful parameter here.
		
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
		getChangeCaller().mediaItemUpdated(sfile);
	}
	
	protected void local_setHashCode (String sfile, BigInteger hashcode) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETHASHCODE);
		int n;
		try {
			if (hashcode != null) {
				ps.setBytes(1, hashcode.toByteArray());
			}
			else {
				ps.setNull(1, java.sql.Types.BLOB);
			}
			ps.setString(2, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for local_setHashCode('"+sfile+"','"+hashcode+"').");
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaTrack setters.
	
	protected void local_trackPlayed (String sfile, long x, Date date) throws DbException, SQLException, ClassNotFoundException {
		PreparedStatement ps;
		
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_TRACKPLAYED);
		int n;
		try {
			ps.setLong(1, x);
			ps.setDate(2, new java.sql.Date(date.getTime()));
			ps.setString(3, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
		getChangeCaller().mediaItemUpdated(sfile);
	}
	
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
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
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
		getChangeCaller().mediaItemUpdated(sfile);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
//	=== === === === === === === === === === === === === === === === === === ===
//	===
//	===     Static methods.
//	===
//	=== === === === === === === === === === === === === === === === === === ===
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem getters.
	
	static private String local_getAllMediaSql (MediaType mediaType, boolean hideMissing, IDbColumn sort, SortDirection direction, String search) {
		StringBuilder sql = new StringBuilder();
		
		sql.append(search == null ? _SQL_MEDIAFILES_SELECT : _SQL_MEDIAFILESTAGS_SELECT); // If we are searching need to join tags table.
		
		if (hideMissing || mediaType != MediaType.UNKNOWN) sql.append(_SQL_WHERE);
		if (mediaType != MediaType.UNKNOWN) {
			sql.append(search == null ? _SQL_MEDIAFILES_WHERTYPE : _SQL_MEDIAFILESTAGS_WHERTYPE); // Type has prefix of 'm' when joining tags DB.
		}
		if (hideMissing) {
			if (mediaType != MediaType.UNKNOWN) sql.append(_SQL_AND);
			sql.append(_SQL_MEDIAFILES_WHERENOTMISSING);
		}
		if (search != null) {
			if (hideMissing || mediaType != MediaType.UNKNOWN) sql.append(_SQL_AND);
			sql.append(_SQL_MEDIAFILES_WHERESEARCHTAGS);
		}
		sql.append(_SQL_ORDERBYREPLACE);
		
//		if (mediaType == MediaType.UNKNOWN) {
//			if (hideMissing) {
//				sql.append(_SQL_WHERE);
//				sql.append(_SQL_MEDIAFILES_WHERENOTMISSING);
//				sql.append(_SQL_ORDERBYREPLACE);
//			} else {
//				sql.append(_SQL_ORDERBYREPLACE);
//			}
//		}
//		else {
//			if (hideMissing) {
//				sql.append(_SQL_WHERE);
//				sql.append(_SQL_MEDIAFILES_WHERTYPE);
//				sql.append(_SQL_AND);
//				sql.append(_SQL_MEDIAFILES_WHERENOTMISSING);
//				sql.append(_SQL_ORDERBYREPLACE);
//			} else {
//				
//				sql.append(_SQL_ORDERBYREPLACE);
//			}
//		}
		
		String sqls = sql.toString();
		
		switch (direction) {
			case ASC:
				sqls = sqls.replace("{DIR}", "ASC");
				break;
				
			case DESC:
				sqls = sqls.replace("{DIR}", "DESC");
				break;
				
			default:
				throw new IllegalArgumentException();
				
		}
		
		String sortTerm = sort.getName();
		if (sort.getSortOpts() != null) {
			sortTerm = sortTerm.concat(sort.getSortOpts());
		}
		sqls = sqls.replace("{COL}", sortTerm);
		
//		System.err.println("sqls=" + sqls);
		
		return sqls;
	}
	
	static private List<IMixedMediaItem> local_parseRecordSet (ResultSet rs, MixedMediaItemFactory itemFactory) throws SQLException {
		/* Apparently I don't need to preset the size of the array,
		 * and using the auto-grow feature is more efficient than
		 * trying to count the length of the record set.
		 */
		List<IMixedMediaItem> ret = new ArrayList<IMixedMediaItem>();
		
		while (rs.next()) {
			IMixedMediaItem mi = createMediaItem(rs, itemFactory);
			ret.add(mi);
		}
		
		return ret;
	}
	
	static protected IMixedMediaItem createMediaItem (ResultSet rs, MixedMediaItemFactory itemFactory) throws SQLException {
		String filePath = rs.getString(SQL_TBL_MEDIAFILES_COL_FILE.getName());
		IMixedMediaItem mi = itemFactory.getNewMediaItem(filePath);
		
		/* The object returned by the itemFactory may not be fresh.
		 * It is important that this method call every possible setter.
		 * Any setter not called will result in stale data remaining.
		 * Not using .reset() as that would not be thread safe.
		 */
		
		int i = rs.getInt(SQL_TBL_MEDIAFILES_COL_TYPE.getName());
		MediaType t = MediaType.parseInt(i);
		mi.setMediaType(t);
		
		mi.setDateAdded(SqliteHelper.readDate(rs, SQL_TBL_MEDIAFILES_COL_DADDED.getName()));
		
		byte[] bytes = rs.getBytes(SQL_TBL_MEDIAFILES_COL_MD5.getName());
		mi.setHashcode(bytes == null ? null : new BigInteger(bytes));
		
		mi.setDateLastModified(SqliteHelper.readDate(rs, SQL_TBL_MEDIAFILES_COL_DMODIFIED.getName()));
		mi.setEnabled(rs.getInt(SQL_TBL_MEDIAFILES_COL_ENABLED.getName()) != 0); // default to true.
		mi.setMissing(rs.getInt(SQL_TBL_MEDIAFILES_COL_MISSING.getName()) == 1); // default to false.
		mi.setDbRowId(rs.getLong(SQL_TBL_MEDIAFILES_COL_ROWID.getName()));
		mi.setRemoteLocation(rs.getString(SQL_TBL_MEDIAFILES_COL_REMLOC.getName()));
		
		if (t == MediaType.TRACK) {
			mi.setStartCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_STARTCNT.getName()));
			mi.setEndCount(rs.getLong(SQL_TBL_MEDIAFILES_COL_ENDCNT.getName()));
			mi.setDuration(rs.getInt(SQL_TBL_MEDIAFILES_COL_DURATION.getName()));
			mi.setDateLastPlayed(SqliteHelper.readDate(rs, SQL_TBL_MEDIAFILES_COL_DLASTPLAY.getName()));
		}
		else {
			mi.setStartCount(0); // TODO extract constant for default value.
			mi.setEndCount(0); // TODO extract constant for default value.
			mi.setDuration(0); // TODO extract constant for default value.
			mi.setDateLastPlayed(null); // TODO extract constant for default value.
		}
		
		if (t == MediaType.PICTURE) {
			mi.setWidth(rs.getInt(SQL_TBL_MEDIAFILES_COL_WIDTH.getName()));
			mi.setHeight(rs.getInt(SQL_TBL_MEDIAFILES_COL_HEIGHT.getName()));
		}
		else {
			mi.setWidth(0); // TODO extract constant for default value.
			mi.setHeight(0); // TODO extract constant for default value.
		}
		
		return mi;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
