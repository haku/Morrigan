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
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMixedMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.SortColumn;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
import com.vaguehope.morrigan.model.media.internal.db.DefaultMediaItemFactory;
import com.vaguehope.morrigan.model.media.internal.db.MediaSqliteLayer;
import com.vaguehope.morrigan.model.media.internal.db.SqliteHelper;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.GeneratedString;

public class MixedMediaSqliteLayer extends MediaSqliteLayer implements IMixedMediaItemStorageLayer {

	private static final Logger LOG = LoggerFactory.getLogger(MixedMediaSqliteLayer.class);

	protected final DefaultMediaItemFactory itemFactory;

	public MixedMediaSqliteLayer (final String dbFilePath, final boolean autoCommit, final DefaultMediaItemFactory itemFactory) throws DbException {
		super(dbFilePath, autoCommit);
		this.itemFactory = itemFactory;
	}

	//	tbl_mediafiles.
	public static final String SQL_TBL_MEDIAFILES_NAME = "tbl_mediafiles";

	public static final IDbColumn[] SQL_TBL_MEDIAFILES_COLS = new IDbColumn[] {
		SQL_TBL_MEDIAFILES_COL_ID,
		SQL_TBL_MEDIAFILES_COL_FILE,
		SQL_TBL_MEDIAFILES_COL_TYPE,
		SQL_TBL_MEDIAFILES_COL_MIMETYPE,
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

	static protected List<IDbColumn> generateSqlTblMediaFilesColumns () {
		final List<IDbColumn> l = new ArrayList<>();
		for (final IDbColumn c : SQL_TBL_MEDIAFILES_COLS) {
			l.add(c);
		}
		return l;
	}

	@Override
	public List<IDbColumn> getMediaTblColumns () {
		return generateSqlTblMediaFilesColumns();
	}

	private static final String _SQL_MEDIAFILES_SELECT =
		"SELECT"
		+ " id, file, type, mimetype, md5, sha1, added, modified, enabled, enabledmodified, missing, remloc"
		+ ",startcnt,endcnt,lastplay,duration"
		+ ",width,height"
		+ " FROM tbl_mediafiles";

	private static final String _SQL_MEDIAFILESALBUMS_SELECT = // TODO FIXME is this the same as _SQL_MEDIAFILES_SELECT?
		"SELECT"
		+ " distinct m.id AS id,m.type AS type,mimetype,file,md5,sha1,added,modified,enabled,enabledmodified,missing,remloc,startcnt,endcnt,lastplay,duration,width,height"
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

	private static final String SQL_TBL_MEDIAFILES_Q_MISSING =
		"SELECT missing FROM tbl_mediafiles WHERE file=?;";

	//	TODO move to helper / where DbColumn is defined?
	// WARNING: consuming code assumes the order of parameters in the generated SQL.
	private final GeneratedString sqlTblMediaFilesAdd = new GeneratedString() {
		@Override
		public String generateString() {
			final StringBuilder sb = new StringBuilder();
			final IDbColumn[] cols = SQL_TBL_MEDIAFILES_COLS;

			sb.append("INSERT INTO tbl_mediafiles (");
			boolean first = true;
			for (final IDbColumn c : cols) {
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
			for (final IDbColumn c : cols) {
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

	private static final String SQL_TBL_MEDIAFILES_SETTYPE =
		"UPDATE tbl_mediafiles SET type=?" +
		" WHERE file=?;";

	private static final String SQL_TBL_MEDIAFILES_SETMIMETYPE =
			"UPDATE tbl_mediafiles SET mimetype=?" +
					" WHERE file=?;";

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

	private static final String SQL_TBL_MEDIAFILES_SETDIMENSIONS =
		"UPDATE tbl_mediafiles SET width=?,height=?" +
		" WHERE file=?;";

	@Override
	protected List<SqlCreateCmd> getTblCreateCmds() {
		final List<SqlCreateCmd> l = super.getTblCreateCmds();

		// Insert at beginning as latter tables will have keys pointing to this one.
		l.add(0, SqliteHelper.generateSql_Create(SQL_TBL_MEDIAFILES_NAME, SQL_TBL_MEDIAFILES_COLS));

		// TODO Add indexes...
//		l.add(new SqlCreateCmd("SELECT name FROM sqlite_master WHERE name='foobar';",
//				"CREATE UNIQUE INDEX ..."));

		return l;
	}

	@Override
	protected void migrateDb() throws DbException {
		super.migrateDb();
		addColumnIfMissing(SQL_TBL_MEDIAFILES_COL_SHA1);
		addColumnIfMissing(SQL_TBL_MEDIAFILES_COL_MIMETYPE);
	}

	private void addColumnIfMissing(final IDbColumn column) throws DbException {
		try (final PreparedStatement p = getDbCon().prepareStatement("SELECT name FROM pragma_table_info('tbl_mediafiles') WHERE name=?;")) {
			p.setString(1, column.getName());
			try (final ResultSet rs = p.executeQuery()) {
				if (rs.next()) return;
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
		LOG.info("Adding column {} to tbl_mediafiles in: {}", column.getName(), getDbFilePath());
		final String sql = "ALTER TABLE tbl_mediafiles ADD COLUMN " + column.getName() + " " + column.getSqlType();
		try (final PreparedStatement p = getDbCon().prepareStatement(sql)) {
			p.execute();
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<IMediaItem> getAllMedia (final SortColumn[] sorts, final SortDirection[] directions, final boolean hideMissing) throws DbException {
		return getMedia(MediaType.UNKNOWN, sorts, directions, hideMissing);
	}

	@Override
	public List<IMediaItem> getMedia (final MediaType mediaType, final SortColumn[] sorts, final SortDirection[] directions, final boolean hideMissing) throws DbException {
		try {
			return SearchParser.parseSearch(mediaType, sorts, directions, hideMissing, false).execute(getDbCon(), this.itemFactory);
		}
		catch (final Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<IMediaItem> getMedia (final MediaType mediaType, final SortColumn[] sorts, final SortDirection[] directions, final boolean hideMissing, final String search) throws DbException {
		try {
			return SearchParser.parseSearch(mediaType, sorts, directions, hideMissing, false, search).execute(getDbCon(), this.itemFactory);
		}
		catch (final Exception e) {
			throw new DbException(e);
		}
	}

	/**
	 * Querying for type UNKNOWN will return all types (i.e. wild-card).
	 */
	@Override
	public List<IMediaItem> search(final MediaType mediaType, final String term, final int maxResults, final SortColumn[] sortColumn, final SortDirection[] sortDirection, final boolean includeDisabled) throws DbException {
		try {
			return SearchParser.parseSearch(mediaType, sortColumn, sortDirection, true, !includeDisabled, term).execute(getDbCon(), this.itemFactory, maxResults);
		}
		catch (final Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public Collection<IMediaItem> getAlbumItems (final MediaType mediaType, final MediaAlbum album) throws DbException {
		List<IMediaItem> ret;

		final StringBuilder sql = new StringBuilder();
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

		try (final PreparedStatement ps = getDbCon().prepareStatement(sql.toString())) {
			if (mediaType != MediaType.UNKNOWN) {
				ps.setInt(1, mediaType.getN());
				ps.setLong(2, album.getDbRowId());
			}
			else {
				ps.setLong(1, album.getDbRowId());
			}
			final ResultSet rs = ps.executeQuery();
			try {
				ret = local_parseRecordSet(rs, this.itemFactory);
			}
			finally {
				rs.close();
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
		return ret;
	}

	@Override
	public FileExistance hasFile (final File file) throws DbException {
		return hasFile(file.getAbsolutePath());
	}

	@Override
	public FileExistance hasFile (final String filePath) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_Q_MISSING)) {
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
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public IMediaItem getByFile (final File file) throws DbException {
		return getByFile(file.getAbsolutePath());
	}

	@Override
	public IMediaItem getByFile (final String filePath) throws DbException {
		final String sql = _SQL_MEDIAFILES_SELECT + _SQL_WHERE + _SQL_MEDIAFILES_WHEREFILEEQ;
		try (final PreparedStatement ps = getDbCon().prepareStatement(sql)) {
			ps.setString(1, filePath);
			ps.setMaxRows(2); // Ask for 1, so we know if there is more than 1.

			final List<IMediaItem> res;
			try (final ResultSet rs = ps.executeQuery()) {
				res = local_parseRecordSet(rs, this.itemFactory);
			}
			if (res.size() == 1) return res.get(0);
			throw new IllegalArgumentException("File not found '" + filePath + "' (results count = " + res.size() + ").");
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public IMediaItem getByMd5 (final BigInteger md5) throws DbException {
		final String sql = _SQL_MEDIAFILES_SELECT + _SQL_WHERE + _SQL_MEDIAFILES_WHERE_MD5_EQ;
		try (final PreparedStatement ps = getDbCon().prepareStatement(sql)) {
			ps.setBytes(1, md5.toByteArray());
			ps.setMaxRows(2); // Ask for 2 so we know if there is more than 1.
			try (final ResultSet rs = ps.executeQuery()) {
				final List<IMediaItem> res = local_parseRecordSet(rs, this.itemFactory);
				if (res.size() == 1) return res.get(0);
				throw new IllegalArgumentException("File not found '" + md5.toString(16) + "' (results count = " + res.size() + ").");
			}
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean addFile (final MediaType mediaType, final File file) throws DbException {
		return addFile(mediaType, file.getAbsolutePath(), file.lastModified());
	}

	@Override
	public boolean addFile (final MediaType mediaType, final String filePath, final long lastModified) throws DbException {
		if (!hasFile(filePath).isKnown()) {
			try (final PreparedStatement ps = getDbCon().prepareStatement(this.sqlTblMediaFilesAdd.toString())) {
				// WARNING: this assumes the order of parameters in the above SQL.
				ps.setString(1, filePath);
				ps.setInt(2, mediaType.getN());
				ps.setDate(3, new java.sql.Date(new Date().getTime()));
				ps.setDate(4, new java.sql.Date(lastModified));
				final int n = ps.executeUpdate();
				if (n < 1) throw new DbException("No update occured for addTrack('" + filePath + "','" + lastModified + "').");
			}
			catch (final SQLException e) {
				throw new DbException(e);
			}
			getChangeEventCaller().mediaItemAdded(filePath);
			return true;
		}
		return false;
	}

	@Override
	public boolean[] addFiles (final MediaType mediaType,final List<File> filesToAdd) throws DbException {
		final boolean[] ret = new boolean[filesToAdd.size()];
		final List<File> unknownFiles = new ArrayList<>(filesToAdd.size());

		for (int i = 0; i < filesToAdd.size(); i++) {
			final File file = filesToAdd.get(i);

			if (file == null) throw new IllegalArgumentException("File can not be null.");

			final String filePath = file.getAbsolutePath();
			if (filePath == null || filePath.isEmpty()) throw new IllegalArgumentException("filePath is null or empty: " + filePath);

			switch (hasFile(filePath)) {
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

		try (final PreparedStatement ps = getDbCon().prepareStatement(this.sqlTblMediaFilesAdd.toString())) {
			for (final File file : unknownFiles) {
				if (file == null) continue; // Ignore placeholders.
				// WARNING: this assumes the order of parameters in the above SQL.
				ps.setString(1, file.getAbsolutePath());
				ps.setInt(2, mediaType.getN());
				ps.setDate(3, new java.sql.Date(new Date().getTime()));
				ps.setDate(4, new java.sql.Date(file.lastModified()));
				ps.addBatch();
			}

			final int[] batchRes = ps.executeBatch();

			final List<File> addedFiles = new ArrayList<>();
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
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	protected void local_renameFile (final String oldPath, final String newPath) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETFILE)) {
			ps.setString(1, newPath);
			ps.setString(2, oldPath);
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_renameFile('" + oldPath + "','" + newPath + "').");
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public int removeFile (final String sfile) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_REMOVE)) {
			ps.setString(1, sfile);
			final int ret = ps.executeUpdate();
			if (ret > 0) getChangeEventCaller().mediaItemRemoved(sfile);
			return ret;
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public int removeFile (final IDbItem dbItem) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_REMOVE_BYROWID)) {
			ps.setLong(1, dbItem.getDbRowId());
			final int ret = ps.executeUpdate();
			if (ret > 0) getChangeEventCaller().mediaItemRemoved(null); // FIXME pass a useful parameter here.
			return ret;
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}

	}

	@Override
	public void setDateAdded (final IMediaItem item, final Date date) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDATEADDED)) {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setDateAdded('" + item + "','" + date + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setMd5 (final IMediaItem item, final BigInteger md5) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SET_MD5)) {
			if (md5 != null) {
				ps.setBytes(1, md5.toByteArray());
			}
			else {
				ps.setNull(1, java.sql.Types.BLOB);
			}
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setMd5('" + item + "','" + md5 + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setSha1(final IMediaItem item, final BigInteger sha1) throws DbException {
		try (final PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SET_SHA1)) {
			if (sha1 != null) {
				ps.setBytes(1, sha1.toByteArray());
			}
			else {
				ps.setNull(1, java.sql.Types.BLOB);
			}
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setSha1('" + item + "','" + sha1 + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setDateLastModified (final IMediaItem item, final Date date) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDMODIFIED)) {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setDateLastModified('" + item + "','" + date + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setEnabled (final IMediaItem item, final boolean value) throws DbException {
		_setEnabled(item, value, true, null);
	}

	@Override
	public void setEnabled (final IMediaItem item, final boolean value, final Date lastModified) throws DbException {
		_setEnabled(item, value, false, lastModified);
	}

	protected void _setEnabled (final IMediaItem item, final boolean value, final boolean nullIsNow, final Date lastModified) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETENABLED)) {
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

			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setEnabled('" + item + "','" + value + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setMissing (final IMediaItem item, final boolean value) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETMISSING)) {
			ps.setInt(1, value ? 1 : 0);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for local_setMissing('" + item + "','" + value + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setRemoteLocation (final IMediaItem item, final String remoteLocation) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETREMLOC)) {
			ps.setString(1, remoteLocation);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured for local_setRemoteLocation('"+item+"','"+remoteLocation+"').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setItemMediaType (final IMediaItem item, final MediaType newType) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETTYPE)) {
			ps.setInt(1, newType.getN());
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setItemMimeType (final IMediaItem item, final String newType) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETMIMETYPE)) {
			ps.setString(1, newType);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackPlayed (final IMediaItem item) throws DbException {
		_trackPlayed(item, 1, new Date());
	}

	protected void _trackPlayed (final IMediaItem item, final long x, final Date date) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_TRACKPLAYED)) {
			ps.setLong(1, x);
			ps.setDate(2, new java.sql.Date(date.getTime()));
			ps.setString(3, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackStartCnt (final IMediaItem item, final long x) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCSTART)) {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackStartCnt (final IMediaItem item, final long x) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETSTART)) {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setDateLastPlayed (final IMediaItem item, final Date date) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDATELASTPLAYED)) {
			ps.setDate(1, new java.sql.Date(date.getTime()));
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackFinished (final IMediaItem item) throws DbException {
		_incTrackEndCnt(item, 1);
	}

	@Override
	public void incTrackEndCnt (final IMediaItem item, final long n) throws DbException {
		_incTrackEndCnt(item, n);
	}

	protected void _incTrackEndCnt (final IMediaItem item, final long x) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_INCEND)) {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackEndCnt (final IMediaItem item, final long x) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETEND);) {
			ps.setLong(1, x);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackDuration (final IMediaItem item, final int duration) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDURATION)) {
			ps.setInt(1, duration);
			ps.setString(2, item.getFilepath());
			final int n = ps.executeUpdate();
			if (n<1) throw new DbException("No update occured.");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setDimensions (final IMediaItem item, final int width, final int height) throws DbException {
		try (PreparedStatement ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDIMENSIONS)) {
			ps.setInt(1, width);
			ps.setInt(2, height);
			ps.setString(3, item.getFilepath());
			int n;
			n = ps.executeUpdate();
			if (n < 1) throw new DbException("No update occured for setRemoteLocation('" + item + "','" + width + "','" + height + "').");
			getChangeEventCaller().mediaItemUpdated(item);
		}
		catch (final SQLException e) {
			throw new DbException(e);
		}
	}

	@Override
	public IMediaItem getNewT (final String filePath) {
		return this.itemFactory.getNewMediaItem(filePath);
	}

	private static class ColumnIndexes {
		final int colFile;
		final int colType;
		final int colMimeType;
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
			this.colMimeType = rs.findColumn(SQL_TBL_MEDIAFILES_COL_MIMETYPE.getName());
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

	protected static List<IMediaItem> local_parseRecordSet (final ResultSet rs, final DefaultMediaItemFactory itemFactory) throws SQLException {
		final List<IMediaItem> ret = new ArrayList<>();
		if (rs.next()) {
			final ColumnIndexes indexes = new ColumnIndexes(rs);
			do {
				ret.add(createMediaItem(rs, indexes, itemFactory));
			}
			while (rs.next());
		}
		return ret;
	}

	protected static IMediaItem createMediaItem (final ResultSet rs, final ColumnIndexes indexes, final DefaultMediaItemFactory itemFactory) throws SQLException {
		final String filePath = rs.getString(indexes.colFile);
		final IMediaItem mi = itemFactory.getNewMediaItem(filePath);

		/* The object returned by the itemFactory may not be fresh.
		 * It is important that this method call every possible setter.
		 * Any setter not called will result in stale data remaining.
		 * Not using .reset() as that would not be thread safe.
		 */

		final int i = rs.getInt(indexes.colType);
		final MediaType t = MediaType.parseInt(i);
		mi.setMediaType(t);

		mi.setMimeType(rs.getString(indexes.colMimeType));

		mi.setDateAdded(SqliteHelper.readDate(rs, indexes.colAdded));

		final byte[] md5Bytes = rs.getBytes(indexes.colMd5);
		mi.setMd5(md5Bytes == null ? null : new BigInteger(md5Bytes));

		final byte[] sha1Bytes = rs.getBytes(indexes.colSha1);
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
