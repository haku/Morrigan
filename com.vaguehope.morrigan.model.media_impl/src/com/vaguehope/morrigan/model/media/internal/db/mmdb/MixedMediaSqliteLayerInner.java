package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMixedMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.internal.db.MediaSqliteLayer;
import com.vaguehope.morrigan.model.media.internal.db.SqliteHelper;
import com.vaguehope.morrigan.util.GeneratedString;
import com.vaguehope.sqlitewrapper.DbException;

public abstract class MixedMediaSqliteLayerInner extends MediaSqliteLayer<IMixedMediaItem> implements IMixedMediaItemStorageLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final MixedMediaItemFactory itemFactory;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected MixedMediaSqliteLayerInner (final String dbFilePath, final boolean autoCommit, final MixedMediaItemFactory itemFactory) throws DbException {
		super(dbFilePath, autoCommit);
		this.itemFactory = itemFactory;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	SQL for defining tables.

//	-  -  -  -  -  -  -  -  -  -  -  -
//	SQL for defining tbl_mediafiles.

	public static final String SQL_TBL_MEDIAFILES_NAME = "tbl_mediafiles";

	public static final IDbColumn[] SQL_TBL_MEDIAFILES_COLS = new IDbColumn[] {
		SQL_TBL_MEDIAFILES_COL_ID,
		SQL_TBL_MEDIAFILES_COL_FILE,
		SQL_TBL_MEDIAFILES_COL_TYPE,
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

	public static IDbColumn parseColumnFromName (final String name) {
		for (IDbColumn c : SQL_TBL_MEDIAFILES_COLS) {
			if (c.getName().equals(name)) {
				return c;
			}
		}
		throw new IllegalArgumentException("Failed to find column from name '"+name+"'.");
	}

	static protected List<IDbColumn> generateSqlTblMediaFilesColumns () {
		List<IDbColumn> l = new ArrayList<IDbColumn>();
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
		+ " id, file, type, md5, added, modified, enabled, missing, remloc"
		+ ",startcnt,endcnt,lastplay,duration"
    	+ ",width,height"
    	+ " FROM tbl_mediafiles";

	private static final String _SQL_MEDIAFILESALBUMS_SELECT = // TODO FIXME is this the same as _SQL_MEDIAFILES_SELECT?
		"SELECT"
		+ " distinct m.id AS id,m.type AS type,file,md5,added,modified,enabled,missing,remloc,startcnt,endcnt,lastplay,duration,width,height"
		+ " FROM tbl_mediafiles AS m";

	private static final String _SQL_WHERE = " WHERE";
	private static final String _SQL_AND = " AND";

	private static final String _SQL_MEDIAFILESTAGS_WHERTYPE =
		" m.type=?";

	private static final String _SQL_MEDIAFILESALBUMS_WHEREALBUM =
		" m.id = tbl_album_items.mf_id AND tbl_album_items.album_id = ?";

	private static final String _SQL_MEDIAFILES_WHERENOTMISSING =
		" (missing<>1 OR missing is NULL)";

	private static final String _SQL_ORDERBYREPLACE =
		" ORDER BY {COL} {DIR};";

	private static final String _SQL_MEDIAFILES_WHEREFILEEQ =
		" file = ?";

//	-  -  -  -  -  -  -  -  -  -  -  -
//	Adding and removing tracks.

	private static final String SQL_TBL_MEDIAFILES_Q_EXISTS =
		"SELECT count(*) FROM tbl_mediafiles WHERE file=? COLLATE NOCASE;";

//	TODO move to helper / where DbColumn is defined?
	// WARNING: consuming code assumes the order of parameters in the generated SQL.
	private final GeneratedString sqlTblMediaFilesAdd = new GeneratedString() {
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
		"DELETE FROM tbl_mediafiles WHERE file=?";

	private static final String SQL_TBL_MEDIAFILES_REMOVE_BYROWID =
		"DELETE FROM tbl_mediafiles WHERE id=?";

//	-  -  -  -  -  -  -  -  -  -  -  -
//	Setting MediaItem data.

	private static final String SQL_TBL_MEDIAFILES_SETDATEADDED =
		"UPDATE tbl_mediafiles SET added=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETHASHCODE =
		"UPDATE tbl_mediafiles SET md5=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETDMODIFIED =
		"UPDATE tbl_mediafiles SET modified=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETENABLED =
		"UPDATE tbl_mediafiles SET enabled=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETMISSING =
		"UPDATE tbl_mediafiles SET missing=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETREMLOC =
		"UPDATE tbl_mediafiles SET remloc=?" +
		" WHERE file=?;";


//	-  -  -  -  -  -  -  -  -  -  -  -
//	Setting MixedMediaItem data.

	private static final String SQL_TBL_MEDIAFILES_SETTYPE =
		"UPDATE tbl_mediafiles SET type=?" +
		" WHERE file=?;";

//	-  -  -  -  -  -  -  -  -  -  -  -
//	Setting MediaTrack data.

	private static final String SQL_TBL_MEDIAFILES_TRACKPLAYED =
		"UPDATE tbl_mediafiles SET startcnt=startcnt+?,lastplay=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_INCSTART =
		"UPDATE tbl_mediafiles SET startcnt=startcnt+?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETSTART =
		"UPDATE tbl_mediafiles SET startcnt=? WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_INCEND =
		"UPDATE tbl_mediafiles SET endcnt=endcnt+?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETEND =
		"UPDATE tbl_mediafiles SET endcnt=? WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETDATELASTPLAYED =
		"UPDATE tbl_mediafiles SET lastplay=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETDURATION =
		"UPDATE tbl_mediafiles SET duration=?" +
		" WHERE file=?;";

//	-  -  -  -  -  -  -  -  -  -  -  -
//	Setting MediaPicture data.

	private static final String SQL_TBL_MEDIAFILES_SETDIMENSIONS =
		"UPDATE tbl_mediafiles SET width=?,height=?" +
		" WHERE file=?;";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Create statements.

	@Override
	protected List<SqlCreateCmd> getTblCreateCmds() {
		List<SqlCreateCmd> l = super.getTblCreateCmds();

		// Insert at beginning as latter tables will have keys pointing to this one.
		l.add(0, SqliteHelper.generateSql_Create(SQL_TBL_MEDIAFILES_NAME, SQL_TBL_MEDIAFILES_COLS));

		return l;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Album readers.

	protected Collection<IMixedMediaItem> local_getAlbumItems (final MediaType mediaType, final MediaAlbum album) throws SQLException, ClassNotFoundException {
		List<IMixedMediaItem> ret;

		StringBuilder sql = new StringBuilder();
		sql.append(_SQL_MEDIAFILESALBUMS_SELECT);
		sql.append(",").append(SQL_TBL_ALBUM_ITEMS);
		sql.append(_SQL_WHERE);
		if (mediaType != MediaType.UNKNOWN) {
			sql.append(_SQL_MEDIAFILESTAGS_WHERTYPE); // type param.
			sql.append(_SQL_AND);
		}
		sql.append(_SQL_MEDIAFILESALBUMS_WHEREALBUM); // album_id param.
		sql.append(_SQL_AND);
		sql.append(_SQL_MEDIAFILES_WHERENOTMISSING);
		sql.append(_SQL_ORDERBYREPLACE.replace("{COL}", SQL_TBL_MEDIAFILES_COL_FILE.getName()).replace("{DIR}", "ASC"));

		PreparedStatement ps = getDbCon().prepareStatement(sql.toString());
		try {
			if (mediaType != MediaType.UNKNOWN) {
				ps.setInt(1, mediaType.getN());
				ps.setLong(2, album.getDbRowId());
			}
			else {
				ps.setLong(1, album.getDbRowId());
			}
			ResultSet rs = ps.executeQuery();
			try {
				ret = local_parseRecordSet(rs, this.itemFactory);
			}
			finally {
				rs.close();
			}
		}
		finally {
			ps.close();
		}
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media queries.

	protected boolean local_hasFile (final String filePath) throws SQLException, ClassNotFoundException {
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

	protected IMixedMediaItem local_getByFile (final String filePath) throws SQLException, ClassNotFoundException {
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

	protected boolean local_addTrack (final MediaType mediaType, final String filePath, final long lastModified) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;

		int n;
		if (!local_hasFile(filePath)) {
			String sql = this.sqlTblMediaFilesAdd.toString();
			ps = getDbCon().prepareStatement(sql);
			try {
				// WARNING: this assumes the order of parameters in the above SQL.
				ps.setString(1, filePath);
				ps.setInt(2, mediaType.getN());
				ps.setDate(3, new java.sql.Date(new Date().getTime()));
				ps.setDate(4, new java.sql.Date(lastModified));
				n = ps.executeUpdate();
			} finally {
				ps.close();
			}
			if (n<1) throw new DbException("No update occured for addTrack('"+filePath+"','"+lastModified+"').");

			getChangeEventCaller().mediaItemAdded(filePath);

			return true;
		}

		return false;
	}

	protected boolean[] local_addFiles (final List<File> files) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		int[] n;

		String sql = this.sqlTblMediaFilesAdd.toString();
		ps = getDbCon().prepareStatement(sql);
		try {
			for (File file : files) {
				if (file == null) throw new IllegalArgumentException("File can not be null.");

				String filePath = file.getAbsolutePath();
				if (filePath == null || filePath.isEmpty()) throw new IllegalArgumentException("filePath is null or empty: " + filePath);

				if (!local_hasFile(filePath)) {
					// WARNING: this assumes the order of parameters in the above SQL.
					ps.setString(1, filePath);
					ps.setInt(2, MediaType.UNKNOWN.getN());
					ps.setDate(3, new java.sql.Date(new Date().getTime()));
					ps.setDate(4, new java.sql.Date(file.lastModified()));
					ps.addBatch();
				}
			}

			n = ps.executeBatch();

			boolean[] b = new boolean[n.length];
			final List<File> added = new ArrayList<File>();
			for (int i = 0; i < n.length; i++) {
				b[i] = (n[i] > 0 || n[i] == Statement.SUCCESS_NO_INFO);
				if (b[i]) added.add(files.get(i));
			}
			getChangeEventCaller().mediaItemsAdded(added);
			return b;
		}
		finally {
			ps.close();
		}
	}

	protected int local_removeTrack (final String sfile) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;

		int ret;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_REMOVE);
		try {
			ps.setString(1, sfile);
			ret = ps.executeUpdate();
		} finally {
			ps.close();
		}

		if (ret == 1) getChangeEventCaller().mediaItemRemoved(sfile);

		return ret;
	}

	protected int local_removeTrack (final IDbItem dbItem) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;

		int ret;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_REMOVE_BYROWID);
		try {
			ps.setLong(1, dbItem.getDbRowId());
			ret = ps.executeUpdate();
		} finally {
			ps.close();
		}

		if (ret == 1) getChangeEventCaller().mediaItemRemoved(null); // FIXME pass a useful parameter here.

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem setters.

	protected void local_setDateAdded (final IMediaItem item, final Date date) throws Exception, ClassNotFoundException {
		PreparedStatement ps;

		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDATEADDED);
		int n;
		try {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n < 1) throw new DbException("No update occured for local_setDateAdded('" + item + "','" + date + "').");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_setHashCode (final IMediaItem item, final BigInteger hashcode) throws SQLException, ClassNotFoundException, DbException {
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
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n < 1) throw new DbException("No update occured for local_setHashCode('" + item + "','" + hashcode + "').");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_setDateLastModified (final IMediaItem item, final Date date) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDMODIFIED);
		int n;
		try {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n < 1) throw new DbException("No update occured for local_setDateLastModified('" + item + "','" + date + "').");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_setEnabled (final IMediaItem item, final boolean value) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETENABLED);
		int n;
		try {
			ps.setInt(1, value ? 1 : 0);
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n < 1) throw new DbException("No update occured for local_setEnabled('" + item + "','" + value + "').");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_setMissing (final IMediaItem item, final boolean value) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETMISSING);
		int n;
		try {
			ps.setInt(1, value ? 1 : 0);
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n < 1) throw new DbException("No update occured for local_setMissing('" + item + "','" + value + "').");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_setRemoteLocation(final IMediaItem item, final String remoteLocation) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETREMLOC);
		int n;
		try {
			ps.setString(1, remoteLocation);
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for local_setRemoteLocation('"+item+"','"+remoteLocation+"').");
		getChangeEventCaller().mediaItemUpdated(item);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MixedMediaItem setters.

	protected void local_setItemMediaType(final IMediaItem item, final MediaType newType) throws DbException, SQLException, ClassNotFoundException {
		PreparedStatement ps;

		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETTYPE);
		int n;
		try {
			ps.setInt(1, newType.getN());
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
		getChangeEventCaller().mediaItemUpdated(item);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaTrack setters.

	protected void local_trackPlayed (final IMediaItem item, final long x, final Date date) throws DbException, SQLException, ClassNotFoundException {
		PreparedStatement ps;

		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_TRACKPLAYED);
		int n;
		try {
			ps.setLong(1, x);
			ps.setDate(2, new java.sql.Date(date.getTime()));
			ps.setString(3, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_incTrackStartCnt (final IMediaItem item, final long x) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;

		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCSTART);
		int n;
		try {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_setTrackStartCnt (final IMediaItem item, final long x) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;

		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETSTART);
		int n;
		try {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_setDateLastPlayed (final IMediaItem item, final Date date) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;

		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDATELASTPLAYED);
		int n;
		try {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_incTrackEndCnt (final IMediaItem item, final long x) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;

		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCEND);
		int n;
		try {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_setTrackEndCnt (final IMediaItem item, final long x) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;

		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETEND);
		int n;
		try {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_setTrackDuration (final IMediaItem item, final int duration) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDURATION);
		int n;
		try {
			ps.setInt(1, duration);
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured.");
		getChangeEventCaller().mediaItemUpdated(item);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaPic setters.

	protected void local_setDimensions(final IMediaItem item, final int width, final int height) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDIMENSIONS);
		int n;
		try {
			ps.setInt(1, width);
			ps.setInt(2, height);
			ps.setString(3, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n < 1) throw new DbException("No update occured for setRemoteLocation('" + item + "','" + width + "','" + height + "').");
		getChangeEventCaller().mediaItemUpdated(item);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

//	=== === === === === === === === === === === === === === === === === === ===
//	===
//	===     Static methods.
//	===
//	=== === === === === === === === === === === === === === === === === === ===

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem getters.

	protected static List<IMixedMediaItem> local_parseRecordSet (final ResultSet rs, final MixedMediaItemFactory itemFactory) throws SQLException {
		final List<IMixedMediaItem> ret = new ArrayList<IMixedMediaItem>();
		while (rs.next()) {
			ret.add(createMediaItem(rs, itemFactory));
		}
		return ret;
	}

	protected static IMixedMediaItem createMediaItem (final ResultSet rs, final MixedMediaItemFactory itemFactory) throws SQLException {
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
		mi.setDbRowId(rs.getLong(SQL_TBL_MEDIAFILES_COL_ID.getName()));
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
