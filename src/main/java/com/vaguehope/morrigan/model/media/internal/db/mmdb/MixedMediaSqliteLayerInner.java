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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMixedMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.internal.db.MediaSqliteLayer;
import com.vaguehope.morrigan.model.media.internal.db.SqliteHelper;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.GeneratedString;

public abstract class MixedMediaSqliteLayerInner extends MediaSqliteLayer<IMixedMediaItem> implements IMixedMediaItemStorageLayer {

	private static final Logger LOG = LoggerFactory.getLogger(MixedMediaSqliteLayerInner.class);

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
		SQL_TBL_MEDIAFILES_COL_SHA1,
		SQL_TBL_MEDIAFILES_COL_DADDED,
		SQL_TBL_MEDIAFILES_COL_DMODIFIED,
		SQL_TBL_MEDIAFILES_COL_ENABLED,
		SQL_TBL_MEDIAFILES_COL_ENABLEDMODIFIED,
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
		+ " id, file, type, md5, sha1, added, modified, enabled, enabledmodified, missing, remloc"
		+ ",startcnt,endcnt,lastplay,duration"
    	+ ",width,height"
    	+ " FROM tbl_mediafiles";

	private static final String _SQL_MEDIAFILESALBUMS_SELECT = // TODO FIXME is this the same as _SQL_MEDIAFILES_SELECT?
		"SELECT"
		+ " distinct m.id AS id,m.type AS type,file,md5,sha1,added,modified,enabled,enabledmodified,missing,remloc,startcnt,endcnt,lastplay,duration,width,height"
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

	private static final String _SQL_MEDIAFILES_WHERE_MD5_EQ =
		" md5 = ?";

//	-  -  -  -  -  -  -  -  -  -  -  -
//	Adding and removing tracks.

	private static final String SQL_TBL_MEDIAFILES_Q_MISSING =
		"SELECT missing FROM tbl_mediafiles WHERE file=?;";

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

	private static final String SQL_TBL_MEDIAFILES_SETFILE =
		"UPDATE tbl_mediafiles SET file=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETDATEADDED =
		"UPDATE tbl_mediafiles SET added=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SET_MD5 =
		"UPDATE tbl_mediafiles SET md5=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SET_SHA1 =
			"UPDATE tbl_mediafiles SET sha1=?" +
					" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETDMODIFIED =
		"UPDATE tbl_mediafiles SET modified=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETENABLED =
		"UPDATE tbl_mediafiles SET enabled=?,enabledmodified=?" +
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

		// TODO Add indexes...
//		l.add(new SqlCreateCmd("SELECT name FROM sqlite_master WHERE name='foobar';",
//				"CREATE UNIQUE INDEX ..."));

		return l;
	}

	@Override
	protected void migrateDb() throws SQLException, ClassNotFoundException {
		super.migrateDb();
		addColumnIfMissing(SQL_TBL_MEDIAFILES_COL_SHA1);
	}

	private void addColumnIfMissing(final IDbColumn column) throws SQLException, ClassNotFoundException {
		try (final PreparedStatement p = getDbCon().prepareStatement("SELECT name FROM pragma_table_info('tbl_mediafiles') WHERE name=?;")) {
			p.setString(1, column.getName());
			try (final ResultSet rs = p.executeQuery()) {
				if (rs.next()) return;
			}
		}
		LOG.info("Adding column {} to tbl_mediafiles in: {}", column.getName(), getDbFilePath());
		final String sql = "ALTER TABLE tbl_mediafiles ADD COLUMN " + column.getName() + " " + column.getSqlType();
		try (final PreparedStatement p = getDbCon().prepareStatement(sql)) {
			p.execute();
		}
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

	protected FileExistance local_hasFile (final String filePath) throws SQLException, ClassNotFoundException, DbException {
		final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_Q_MISSING);
		try {
			ps.setString(1, filePath);
			final ResultSet rs = ps.executeQuery();
			try {
				if (rs.next()) {
					final boolean missing = rs.getInt(1) == 1; // default to false.
					if (rs.next()) throw new DbException(String.format("Path %s in DB more than once.", filePath));
					return missing ? FileExistance.MISSING : FileExistance.EXISTS;
				}
				return FileExistance.UNKNOWN;
			}
			finally {
				rs.close();
			}
		}
		finally {
			ps.close();
		}
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

	protected IMixedMediaItem local_getByMd5 (final BigInteger md5) throws SQLException, ClassNotFoundException {
		PreparedStatement ps;
		ResultSet rs;
		List<IMixedMediaItem> res;

		String sql = _SQL_MEDIAFILES_SELECT + _SQL_WHERE + _SQL_MEDIAFILES_WHERE_MD5_EQ;
		ps = getDbCon().prepareStatement(sql);
		try {
			ps.setBytes(1, md5.toByteArray());
			ps.setMaxRows(2); // Ask for 2 so we know if there is more than 1.

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
		throw new IllegalArgumentException("File not found '" + md5.toString(16) + "' (results count = " + res.size() + ").");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media adders and removers.

	protected boolean local_addTrack (final MediaType mediaType, final String filePath, final long lastModified) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;

		int n;
		if (!local_hasFile(filePath).isKnown()) {
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

	protected boolean[] local_addFiles (final List<File> filesToAdd) throws SQLException, ClassNotFoundException, DbException {
		final boolean[] ret = new boolean[filesToAdd.size()];
		final List<File> unknownFiles = new ArrayList<File>(filesToAdd.size());

		for (int i = 0; i < filesToAdd.size(); i++) {
			final File file = filesToAdd.get(i);

			if (file == null) throw new IllegalArgumentException("File can not be null.");

			final String filePath = file.getAbsolutePath();
			if (filePath == null || filePath.isEmpty()) throw new IllegalArgumentException("filePath is null or empty: " + filePath);

			switch (local_hasFile(filePath)) {
				case UNKNOWN:
					unknownFiles.add(file);
					break;
				case MISSING:
					// A long long time ago, I thought it would be a good idea to make the file column NOCASE.
					// This means if a path changes case, the new file can not be inserted along side the old one.
					// Hopefully it will not happen to often, so not worried about performance.
					local_renameFile(filePath, filePath);
					ret[i] = true;
					unknownFiles.add(null); // Placeholder.
					break;
				default:
					ret[i] = false;
					unknownFiles.add(null); // Placeholder.
					break;
			}
		}

		if (filesToAdd.size() != unknownFiles.size()) throw new IllegalStateException("filesToAdd.size() != unknownFiles.size()");

		final PreparedStatement ps = getDbCon().prepareStatement(this.sqlTblMediaFilesAdd.toString());
		try {
			for (final File file : unknownFiles) {
				if (file == null) continue; // Ignore placeholders.
				// WARNING: this assumes the order of parameters in the above SQL.
				ps.setString(1, file.getAbsolutePath());
				ps.setInt(2, MediaType.UNKNOWN.getN());
				ps.setDate(3, new java.sql.Date(new Date().getTime()));
				ps.setDate(4, new java.sql.Date(file.lastModified()));
				ps.addBatch();
			}

			final int[] batchRes = ps.executeBatch();

			final List<File> addedFiles = new ArrayList<File>();
			int ri = 0;

			for (int fi = 0; fi < unknownFiles.size(); fi++) {
				final File file = unknownFiles.get(fi);
				if (file == null) continue;

				final int result = batchRes[ri];
				ri++;

				final boolean successfullyAdded = (result > 0 || result == Statement.SUCCESS_NO_INFO);
				if (successfullyAdded) addedFiles.add(file);
				ret[fi] = successfullyAdded;
			}

			getChangeEventCaller().mediaItemsAdded(addedFiles);

			return ret;
		}
		finally {
			ps.close();
		}
	}

	protected void local_renameFile (final String oldPath, final String newPath) throws SQLException, ClassNotFoundException, DbException {
		final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETFILE);
		try {
			ps.setString(1, newPath);
			ps.setString(2, oldPath);
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_renameFile('" + oldPath + "','" + newPath + "').");
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

		if (ret > 0) getChangeEventCaller().mediaItemRemoved(sfile);

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

		if (ret > 0) getChangeEventCaller().mediaItemRemoved(null); // FIXME pass a useful parameter here.

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

	protected void local_setMd5 (final IMediaItem item, final BigInteger md5) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SET_MD5);
		int n;
		try {
			if (md5 != null) {
				ps.setBytes(1, md5.toByteArray());
			}
			else {
				ps.setNull(1, java.sql.Types.BLOB);
			}
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n < 1) throw new DbException("No update occured for local_setMd5('" + item + "','" + md5 + "').");
		getChangeEventCaller().mediaItemUpdated(item);
	}

	protected void local_setSha1(final IMediaItem item, final BigInteger sha1) throws SQLException, ClassNotFoundException, DbException {
		int n;
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SET_SHA1)) {
			if (sha1 != null) {
				ps.setBytes(1, sha1.toByteArray());
			}
			else {
				ps.setNull(1, java.sql.Types.BLOB);
			}
			ps.setString(2, item.getFilepath());
			n = ps.executeUpdate();
		}
		if (n < 1) throw new DbException("No update occured for local_setSha1('" + item + "','" + sha1 + "').");
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

	protected void local_setEnabled (final IMediaItem item, final boolean value, final boolean nullIsNow, final Date lastModified) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETENABLED);
		int n;
		try {
			ps.setInt(1, value ? 1 : 0);

			if (lastModified != null) {
				ps.setDate(2, new java.sql.Date(lastModified.getTime()));
			}
			else if (nullIsNow) {
				ps.setDate(2, new java.sql.Date(System.currentTimeMillis()));
			}
			else {
				ps.setNull(2, java.sql.Types.DATE);
			}

			ps.setString(3, item.getFilepath());

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

	private static class ColumnIndexes {
		final int colFile;
		final int colType;
		final int colAdded;
		final int colMd5;
		final int colSha1;
		final int colModified;
		final int colEnabled;
		final int colEnabledModified;
		final int colMissing;
		final int colId;
		final int colRemLoc;
		final int colStartCount;
		final int colEndCount;
		final int colDuration;
		final int colLastPlay;
		final int colWidth;
		final int colHeight;

		public ColumnIndexes (final ResultSet rs) throws SQLException {
			this.colFile = rs.findColumn(SQL_TBL_MEDIAFILES_COL_FILE.getName());
			this.colType = rs.findColumn(SQL_TBL_MEDIAFILES_COL_TYPE.getName());
			this.colAdded = rs.findColumn(SQL_TBL_MEDIAFILES_COL_DADDED.getName());
			this.colMd5 = rs.findColumn(SQL_TBL_MEDIAFILES_COL_MD5.getName());
			this.colSha1 = rs.findColumn(SQL_TBL_MEDIAFILES_COL_SHA1.getName());
			this.colModified = rs.findColumn(SQL_TBL_MEDIAFILES_COL_DMODIFIED.getName());
			this.colEnabled = rs.findColumn(SQL_TBL_MEDIAFILES_COL_ENABLED.getName());
			this.colEnabledModified = rs.findColumn(SQL_TBL_MEDIAFILES_COL_ENABLEDMODIFIED.getName());
			this.colMissing = rs.findColumn(SQL_TBL_MEDIAFILES_COL_MISSING.getName());
			this.colId = rs.findColumn(SQL_TBL_MEDIAFILES_COL_ID.getName());
			this.colRemLoc = rs.findColumn(SQL_TBL_MEDIAFILES_COL_REMLOC.getName());
			this.colStartCount = rs.findColumn(SQL_TBL_MEDIAFILES_COL_STARTCNT.getName());
			this.colEndCount = rs.findColumn(SQL_TBL_MEDIAFILES_COL_ENDCNT.getName());
			this.colDuration = rs.findColumn(SQL_TBL_MEDIAFILES_COL_DURATION.getName());
			this.colLastPlay = rs.findColumn(SQL_TBL_MEDIAFILES_COL_DLASTPLAY.getName());
			this.colWidth = rs.findColumn(SQL_TBL_MEDIAFILES_COL_WIDTH.getName());
			this.colHeight = rs.findColumn(SQL_TBL_MEDIAFILES_COL_HEIGHT.getName());
		}
	}

	protected static List<IMixedMediaItem> local_parseRecordSet (final ResultSet rs, final MixedMediaItemFactory itemFactory) throws SQLException {
		final List<IMixedMediaItem> ret = new ArrayList<IMixedMediaItem>();
		if (rs.next()) {
			final ColumnIndexes indexes = new ColumnIndexes(rs);
			do {
				ret.add(createMediaItem(rs, indexes, itemFactory));
			}
			while (rs.next());
		}
		return ret;
	}

	protected static IMixedMediaItem createMediaItem (final ResultSet rs, final ColumnIndexes indexes, final MixedMediaItemFactory itemFactory) throws SQLException {
		String filePath = rs.getString(indexes.colFile);
		IMixedMediaItem mi = itemFactory.getNewMediaItem(filePath);

		/* The object returned by the itemFactory may not be fresh.
		 * It is important that this method call every possible setter.
		 * Any setter not called will result in stale data remaining.
		 * Not using .reset() as that would not be thread safe.
		 */

		int i = rs.getInt(indexes.colType);
		MediaType t = MediaType.parseInt(i);
		mi.setMediaType(t);

		mi.setDateAdded(SqliteHelper.readDate(rs, indexes.colAdded));

		byte[] md5Bytes = rs.getBytes(indexes.colMd5);
		mi.setMd5(md5Bytes == null ? null : new BigInteger(md5Bytes));

		byte[] sha1Bytes = rs.getBytes(indexes.colSha1);
		mi.setSha1(sha1Bytes == null ? null : new BigInteger(sha1Bytes));

		mi.setDateLastModified(SqliteHelper.readDate(rs, indexes.colModified));
		mi.setEnabled(rs.getInt(indexes.colEnabled) != 0, // default to true.
				SqliteHelper.readDate(rs, indexes.colEnabledModified));
		mi.setMissing(rs.getInt(indexes.colMissing) == 1); // default to false.
		mi.setDbRowId(rs.getLong(indexes.colId));
		mi.setRemoteLocation(rs.getString(indexes.colRemLoc));

		if (t == MediaType.TRACK) {
			mi.setStartCount(rs.getLong(indexes.colStartCount));
			mi.setEndCount(rs.getLong(indexes.colEndCount));
			mi.setDuration(rs.getInt(indexes.colDuration));
			mi.setDateLastPlayed(SqliteHelper.readDate(rs, indexes.colLastPlay));
		}
		else {
			mi.setStartCount(0); // TODO extract constant for default value.
			mi.setEndCount(0); // TODO extract constant for default value.
			mi.setDuration(0); // TODO extract constant for default value.
			mi.setDateLastPlayed(null); // TODO extract constant for default value.
		}

		if (t == MediaType.PICTURE) {
			mi.setWidth(rs.getInt(indexes.colWidth));
			mi.setHeight(rs.getInt(indexes.colHeight));
		}
		else {
			mi.setWidth(0); // TODO extract constant for default value.
			mi.setHeight(0); // TODO extract constant for default value.
		}

		return mi;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
