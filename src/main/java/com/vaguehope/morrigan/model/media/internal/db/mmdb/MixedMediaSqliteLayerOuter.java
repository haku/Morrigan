package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.StringHelper;

public class MixedMediaSqliteLayerOuter extends MixedMediaSqliteLayerInner {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private final MixedMediaItemFactory itemFactory;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.

	public MixedMediaSqliteLayerOuter (final String dbFilePath, final boolean autoCommit, final MixedMediaItemFactory itemFactory) throws DbException {
		super(dbFilePath, autoCommit, itemFactory);
		this.itemFactory = itemFactory;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public List<IDbColumn> getMediaTblColumns () {
		return generateSqlTblMediaFilesColumns();
	}

	@Override
	public IDbColumn getDefaultSortColumn () {
		return SQL_TBL_MEDIAFILES_COL_FILE;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Read methods for IMediaMixedItem.

	@Override
	public List<IMediaItem> getAllMedia (final IDbColumn[] sorts, final SortDirection[] directions, final boolean hideMissing) throws DbException {
		return getMedia(MediaType.UNKNOWN, sorts, directions, hideMissing);
	}

	@Override
	public List<IMediaItem> getMedia (final MediaType mediaType, final IDbColumn[] sorts, final SortDirection[] directions, final boolean hideMissing) throws DbException {
		try {
			return SearchParser.parseSearch(mediaType, sorts, directions, hideMissing, false).execute(getDbCon(), this.itemFactory);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<IMediaItem> getMedia (final MediaType mediaType, final IDbColumn[] sorts, final SortDirection[] directions, final boolean hideMissing, final String search) throws DbException {
		try {
			return SearchParser.parseSearch(mediaType, sorts, directions, hideMissing, false, search).execute(getDbCon(), this.itemFactory);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	/**
	 * Querying for type UNKNOWN will return all types (i.e. wild-card).
	 */
	@Override
	public List<IMediaItem> search(final MediaType mediaType, final String term, final int maxResults) throws DbException {
		try {
			return SearchParser.parseSearch(mediaType, term).execute(getDbCon(), this.itemFactory, maxResults);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<IMediaItem> search(final MediaType mediaType, final String term, final int maxResults, final IDbColumn[] sortColumn, final SortDirection[] sortDirection, final boolean includeDisabled) throws DbException {
		try {
			return SearchParser.parseSearch(mediaType, sortColumn, sortDirection, true, !includeDisabled, term).execute(getDbCon(), this.itemFactory, maxResults);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Album readers.

	@Override
	public Collection<IMediaItem> getAlbumItems (final MediaType mediaType, final MediaAlbum album) throws DbException {
		try {
			return local_getAlbumItems(mediaType, album);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media queries.

	@Override
	public FileExistance hasFile (final File file) throws DbException {
		try {
			return local_hasFile(file.getAbsolutePath());
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public FileExistance hasFile (final String filePath) throws DbException {
		try {
			return local_hasFile(filePath);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public IMediaItem getByFile (final File file) throws DbException {
		try {
			return local_getByFile(file.getAbsolutePath());
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public IMediaItem getByFile (final String filePath) throws DbException {
		try {
			return local_getByFile(filePath);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public IMediaItem getByMd5 (final BigInteger md5) throws DbException {
		try {
			return local_getByMd5(md5);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media adders and removers.

	@Override
	public boolean addFile (final MediaType mediaType, final File file) throws DbException {
		try {
			return local_addTrack(mediaType, file.getAbsolutePath(), file.lastModified());
		}
		catch (Exception e) {
			this.logger.log(Level.SEVERE, "Exception while adding file: " + file.getAbsolutePath(), e);
			throw new DbException(e);
		}
	}

	@Override
	public boolean addFile (final MediaType mediaType, final String filepath, final long lastModified) throws DbException {
		try {
			return local_addTrack(mediaType, filepath, lastModified);
		}
		catch (Exception e) {
			this.logger.log(Level.SEVERE, "Exception while adding file: " + filepath, e);
			throw new DbException(e);
		}
	}

	@Override
	public boolean[] addFiles (final MediaType mediaType,final List<File> files) throws DbException {
		try {
			return local_addFiles(mediaType, files);
		}
		catch (Exception e) {
			this.logger.log(Level.SEVERE, "Exception while adding files: \n" + StringHelper.joinCollection(files, "\n"), e);
			throw new DbException(e);
		}
	}

	@Override
	public int removeFile (final String sfile) throws DbException {
		try {
			return local_removeTrack(sfile);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public int removeFile (final IDbItem dbItem) throws DbException {
		try {
			return local_removeTrack(dbItem);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem setters.

	@Override
	public void setDateAdded (final IMediaItem item, final Date date) throws DbException {
		try {
			local_setDateAdded(item, date);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setMd5 (final IMediaItem item, final BigInteger md5) throws DbException {
		try {
			local_setMd5(item, md5);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setSha1(final IMediaItem item, final BigInteger sha1) throws DbException {
		try {
			local_setSha1(item, sha1);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setDateLastModified (final IMediaItem item, final Date date) throws DbException {
		try {
			local_setDateLastModified(item, date);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setEnabled (final IMediaItem item, final boolean value) throws DbException {
		try {
			local_setEnabled(item, value, true, null);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setEnabled (final IMediaItem item, final boolean value, final Date lastModified) throws DbException {
		try {
			local_setEnabled(item, value, false, lastModified);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setMissing (final IMediaItem item, final boolean value) throws DbException {
		try {
			local_setMissing(item, value);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setRemoteLocation (final IMediaItem item, final String remoteLocation) throws DbException {
		try {
			local_setRemoteLocation(item, remoteLocation);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MixedMediaItem setters.

	@Override
	public void setItemMediaType (final IMediaItem item, final MediaType newType) throws DbException {
		try {
			local_setItemMediaType(item, newType);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setItemMimeType (final IMediaItem item, final String newType) throws DbException {
		try {
			local_setItemMimeType(item, newType);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaTrack setters.

	@Override
	public void incTrackPlayed (final IMediaItem item) throws DbException {
		try {
			local_trackPlayed(item, 1, new Date());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackFinished (final IMediaItem item) throws DbException {
		try {
			local_incTrackEndCnt(item, 1);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackStartCnt (final IMediaItem item, final long n) throws DbException {
		try {
			local_incTrackStartCnt(item, n);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackStartCnt (final IMediaItem item, final long n) throws DbException {
		try {
			local_setTrackStartCnt(item, n);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackEndCnt (final IMediaItem item, final long n) throws DbException {
		try {
			local_incTrackEndCnt(item, n);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackEndCnt (final IMediaItem item, final long n) throws DbException {
		try {
			local_setTrackEndCnt(item, n);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setDateLastPlayed (final IMediaItem item, final Date date) throws DbException {
		try {
			local_setDateLastPlayed(item, date);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackDuration (final IMediaItem item, final int duration) throws DbException {
		try {
			local_setTrackDuration(item, duration);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaPic setters.

	@Override
	public void setDimensions (final IMediaItem item, final int width, final int height) throws DbException {
		try {
			local_setDimensions(item, width, height);
		}
		catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public IMediaItem getNewT (final String filePath) {
		return this.itemFactory.getNewMediaItem(filePath);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
