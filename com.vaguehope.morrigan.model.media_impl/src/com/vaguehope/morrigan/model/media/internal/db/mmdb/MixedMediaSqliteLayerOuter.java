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
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMixedMediaStorageLayer;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.internal.Defaults;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.sqlitewrapper.DbException;

public class MixedMediaSqliteLayerOuter extends MixedMediaSqliteLayerInner implements IMixedMediaStorageLayer {
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
	public List<IDbColumn> getMediaTblColumns() {
		return generateSqlTblMediaFilesColumns();
	}

	@Override
	public IDbColumn getDefaultSortColumn() {
		return SQL_TBL_MEDIAFILES_COL_FILE;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Read methods for IMediaMixedItem.

	private MediaType defaultMediaType = Defaults.MEDIA_ASPECT;

	@Override
	public void setDefaultMediaType (final MediaType mediaType) {
		this.defaultMediaType = mediaType;
	}
	@Override
	public MediaType getDefaultMediaType() {
		return this.defaultMediaType;
	}

	@Override
	public List<IMixedMediaItem> getAllMedia (final IDbColumn sort, final SortDirection direction, final boolean hideMissing) throws DbException {
		return getMedia(MediaType.UNKNOWN, sort, direction, hideMissing);
	}

	@Override
	public List<IMixedMediaItem> getMedia(final IDbColumn sort, final SortDirection direction, final boolean hideMissing) throws DbException {
		return getMedia(getDefaultMediaType(), sort, direction, hideMissing);
	}

	@Override
	public List<IMixedMediaItem> getMedia(final IDbColumn sort, final SortDirection direction, final boolean hideMissing, final String search, final String searchEsc) throws DbException {
		return getMedia(getDefaultMediaType(), sort, direction, hideMissing, search, searchEsc);
	}

	@Override
	public List<IMixedMediaItem> simpleSearch(final String term, final String esc, final int maxResults) throws DbException {
		return simpleSearchMedia(getDefaultMediaType(), term, esc, maxResults);
	}

	@Override
	public List<IMixedMediaItem> getMedia(final MediaType mediaType, final IDbColumn sort, final SortDirection direction, final boolean hideMissing) throws DbException {
		try {
			return local_getAllMedia(mediaType, sort, direction, hideMissing);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<IMixedMediaItem> getMedia(final MediaType mediaType, final IDbColumn sort, final SortDirection direction, final boolean hideMissing, final String search, final String searchEsc) throws DbException {
		try {
			return local_getAllMedia(mediaType, sort, direction, hideMissing, search, searchEsc);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public List<IMixedMediaItem> simpleSearchMedia(final MediaType mediaType, final String term, final String esc, final int maxResults) throws DbException {
		try {
			return local_simpleSearch(mediaType, term, esc, maxResults);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Album readers.

	@Override
	public Collection<IMixedMediaItem> getAlbumItems (final MediaAlbum album) throws DbException {
		return getAlbumItems(MediaType.UNKNOWN, album);
	}

	@Override
	public Collection<IMixedMediaItem> getAlbumItems (final MediaType mediaType, final MediaAlbum album) throws DbException {
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
	public boolean hasFile(final File file) throws DbException {
		try {
			return local_hasFile(file.getAbsolutePath());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public boolean hasFile(final String filePath) throws DbException {
		try {
			return local_hasFile(filePath);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public IMixedMediaItem getByFile(final File file) throws DbException {
		try {
			return local_getByFile(file.getAbsolutePath());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public IMixedMediaItem getByFile(final String filePath) throws DbException {
		try {
			return local_getByFile(filePath);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media adders and removers.

	@Override
	public boolean addFile(final File file) throws DbException {
		return addFile(MediaType.UNKNOWN, file);
	}

	@Override
	public boolean addFile(final String filepath, final long lastModified) throws DbException {
		return addFile(MediaType.UNKNOWN, filepath, lastModified);
	}

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
	public boolean[] addFiles(final List<File> files) throws DbException {
		try {
			return local_addFiles(files);
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
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public int removeFile (final IDbItem dbItem) throws DbException {
		try {
			return local_removeTrack(dbItem);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem setters.

	@Override
	public void setDateAdded (final String sfile, final Date date) throws DbException {
		try {
			local_setDateAdded(sfile, date);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setHashcode (final String sfile, final BigInteger hashcode) throws DbException {
		try {
			local_setHashCode(sfile, hashcode);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setDateLastModified (final String sfile, final Date date) throws DbException {
		try {
			local_setDateLastModified(sfile, date);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setEnabled (final String sfile, final boolean value) throws DbException {
		try {
			local_setEnabled(sfile, value);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setMissing (final String sfile, final boolean value) throws DbException {
		try {
			local_setMissing(sfile, value);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setRemoteLocation (final String sfile, final String remoteLocation) throws DbException {
		try {
			local_setRemoteLocation(sfile, remoteLocation);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MixedMediaItem setters.

	@Override
	public void setItemMediaType(final String sfile, final MediaType newType) throws DbException {
		try {
			local_setItemMediaType(sfile, newType);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaTrack setters.

	@Override
	public void incTrackPlayed (final String sfile) throws DbException {
		try {
			local_trackPlayed(sfile, 1, new Date());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackFinished (final String sfile) throws DbException {
		try {
			local_incTrackEndCnt(sfile, 1);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackStartCnt (final String sfile, final long n) throws DbException {
		try {
			local_incTrackStartCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackStartCnt (final String sfile, final long n) throws DbException {
		try {
			local_setTrackStartCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void incTrackEndCnt (final String sfile, final long n) throws DbException {
		try {
			local_incTrackEndCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackEndCnt (final String sfile, final long n) throws DbException {
		try {
			local_setTrackEndCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setDateLastPlayed (final String sfile, final Date date) throws DbException {
		try {
			local_setDateLastPlayed(sfile, date);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setTrackDuration (final String sfile, final int duration) throws DbException {
		try {
			local_setTrackDuration(sfile, duration);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaPic setters.

	@Override
	public void setDimensions (final String sfile, final int width, final int height) throws DbException {
		try {
			local_setDimensions(sfile, width, height);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public IMixedMediaItem getNewT(final String filePath) {
		return this.itemFactory.getNewMediaItem(filePath);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
