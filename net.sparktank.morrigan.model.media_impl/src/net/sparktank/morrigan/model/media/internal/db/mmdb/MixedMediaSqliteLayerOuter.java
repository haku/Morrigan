package net.sparktank.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sparktank.morrigan.model.db.IDbColumn;
import net.sparktank.morrigan.model.db.IDbItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.media.internal.Defaults;
import net.sparktank.morrigan.model.media.IMixedMediaStorageLayer;
import net.sparktank.morrigan.util.StringHelper;
import net.sparktank.sqlitewrapper.DbException;

public class MixedMediaSqliteLayerOuter extends MixedMediaSqliteLayerInner implements IMixedMediaStorageLayer<IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private final MixedMediaItemFactory itemFactory;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	protected MixedMediaSqliteLayerOuter (String dbFilePath, boolean autoCommit, MixedMediaItemFactory itemFactory) throws DbException {
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
	public void setDefaultMediaType (MediaType mediaType) {
		this.defaultMediaType = mediaType;
	}
	@Override
	public MediaType getDefaultMediaType() {
		return this.defaultMediaType;
	}
	
	@Override
	public List<IMixedMediaItem> getAllMedia(IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		return getAllMedia(MediaType.UNKNOWN, sort, direction, hideMissing);
	}
	
	@Override
	public List<IMixedMediaItem> getAllMedia(IDbColumn sort, net.sparktank.morrigan.model.media.IMediaItemStorageLayer.SortDirection direction, boolean hideMissing, String search, String searchEsc) throws DbException {
		return getAllMedia(MediaType.UNKNOWN, sort, direction, hideMissing, search, searchEsc);
	}
	
	@Override
	public List<IMixedMediaItem> simpleSearch(String term, String esc, int maxResults) throws DbException {
		return simpleSearchMedia(this.defaultMediaType, term, esc, maxResults);
	}
	
	@Override
	public List<IMixedMediaItem> getAllMedia(MediaType mediaType, IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		try {
			return local_getAllMedia(mediaType, sort, direction, hideMissing);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public List<IMixedMediaItem> getAllMedia(MediaType mediaType, IDbColumn sort, net.sparktank.morrigan.model.media.IMediaItemStorageLayer.SortDirection direction, boolean hideMissing, String search, String searchEsc) throws DbException {
		try {
			return local_getAllMedia(mediaType, sort, direction, hideMissing, search, searchEsc);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public List<IMixedMediaItem> simpleSearchMedia(MediaType mediaType, String term, String esc, int maxResults) throws DbException {
		try {
			return local_simpleSearch(mediaType, term, esc, maxResults);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media queries.
	
	@Override
	public boolean hasFile(File file) throws DbException {
		try {
			return local_hasFile(file.getAbsolutePath());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public IMixedMediaItem getByFile(File file) throws DbException {
		try {
			return local_getByFile(file.getAbsolutePath());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Media adders and removers.
	

	@Override
	public boolean addFile(File file) throws DbException {
		return addFile(MediaType.UNKNOWN, file);
	}
	
	@Override
	public boolean addFile(String filepath, long lastModified) throws DbException {
		return addFile(MediaType.UNKNOWN, filepath, lastModified);
	}
	
	@Override
	public boolean addFile (MediaType mediaType, File file) throws DbException {
		try {
			return local_addTrack(mediaType, file.getAbsolutePath(), file.lastModified());
		}
		catch (Exception e) {
			this.logger.log(Level.SEVERE, "Exception while adding file: " + file.getAbsolutePath(), e);
			throw new DbException(e);
		}
	}
	
	@Override
	public boolean addFile (MediaType mediaType, String filepath, long lastModified) throws DbException {
		try {
			return local_addTrack(mediaType, filepath, lastModified);
		}
		catch (Exception e) {
			this.logger.log(Level.SEVERE, "Exception while adding file: " + filepath, e);
			throw new DbException(e);
		}
	}
	
	@Override
	public boolean[] addFiles(List<File> files) throws DbException {
		try {
			return local_addFiles(files);
		}
		catch (Exception e) {
			this.logger.log(Level.SEVERE, "Exception while adding files: " + StringHelper.joinCollection(files, File.pathSeparator), e);
			throw new DbException(e);
		}
	}
	
	@Override
	public int removeFile (String sfile) throws DbException {
		try {
			return local_removeTrack(sfile);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public int removeFile (IDbItem dbItem) throws DbException {
		try {
			return local_removeTrack(dbItem);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaItem setters.
	
	@Override
	public void setDateAdded (String sfile, Date date) throws DbException {
		try {
			local_setDateAdded(sfile, date);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void setHashcode (String sfile, BigInteger hashcode) throws DbException {
		try {
			local_setHashCode(sfile, hashcode);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void setDateLastModified (String sfile, Date date) throws DbException {
		try {
			local_setDateLastModified(sfile, date);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void setEnabled (String sfile, boolean value) throws DbException {
		try {
			local_setEnabled(sfile, value);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void setMissing (String sfile, boolean value) throws DbException {
		try {
			local_setMissing(sfile, value);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void setRemoteLocation (String sfile, String remoteLocation) throws DbException {
		try {
			local_setRemoteLocation(sfile, remoteLocation);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MixedMediaItem setters.
	
	@Override
	public void setItemMediaType(String sfile, MediaType newType) throws DbException {
		try {
			local_setItemMediaType(sfile, newType);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaTrack setters.
	
	@Override
	public void incTrackPlayed (String sfile) throws DbException {
		try {
			local_trackPlayed(sfile, 1, new Date());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void incTrackFinished (String sfile) throws DbException {
		try {
			local_incTrackEndCnt(sfile, 1);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void incTrackStartCnt (String sfile, long n) throws DbException {
		try {
			local_incTrackStartCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void setTrackStartCnt (String sfile, long n) throws DbException {
		try {
			local_setTrackStartCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void incTrackEndCnt (String sfile, long n) throws DbException {
		try {
			local_incTrackEndCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void setTrackEndCnt (String sfile, long n) throws DbException {
		try {
			local_setTrackEndCnt(sfile, n);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void setDateLastPlayed (String sfile, Date date) throws DbException {
		try {
			local_setDateLastPlayed(sfile, date);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public void setTrackDuration (String sfile, int duration) throws DbException {
		try {
			local_setTrackDuration(sfile, duration);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MediaPic setters.
	
	@Override
	public void setDimensions (String sfile, int width, int height) throws DbException {
		try {
			local_setDimensions(sfile, width, height);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public IMixedMediaItem getNewT(String filePath) {
		return this.itemFactory.getNewMediaItem(filePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
