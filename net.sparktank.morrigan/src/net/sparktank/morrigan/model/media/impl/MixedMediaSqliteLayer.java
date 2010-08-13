package net.sparktank.morrigan.model.media.impl;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.morrigan.model.db.interfaces.IDbItem;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaStorageLayer;
import net.sparktank.sqlitewrapper.DbException;

public class MixedMediaSqliteLayer extends MixedMediaSqliteLayerImpl implements IMixedMediaStorageLayer<IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static class MixedMediaSqliteLayerFactory extends RecyclingFactory<IMixedMediaStorageLayer<IMixedMediaItem>, String, Void, DbException> {
		
		MixedMediaSqliteLayerFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(IMixedMediaStorageLayer<IMixedMediaItem> product) {
			return true;
		}
		
		@Override
		protected IMixedMediaStorageLayer<IMixedMediaItem> makeNewProduct(String material) throws DbException {
			return new MixedMediaSqliteLayer(material);
		}
		
	}
	
	public static final MixedMediaSqliteLayerFactory FACTORY = new MixedMediaSqliteLayerFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	protected MixedMediaSqliteLayer (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Read methods for IMediaMixedItem.
	
	@Override
	public List<IMixedMediaItem> getAllMedia(IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		return getAllMedia(MediaType.UNKNOWN, sort, direction, hideMissing);
	}
	
	@Override
	public List<IMixedMediaItem> updateListOfAllMedia(List<IMixedMediaItem> list, IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		return updateListOfAllMedia(MediaType.UNKNOWN, list, sort, direction, hideMissing);
	}
	
	@Override
	public List<IMixedMediaItem> simpleSearch(String term, String esc, int maxResults) throws DbException {
		return simpleSearchMedia(MediaType.UNKNOWN, term, esc, maxResults);
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
	public List<IMixedMediaItem> updateListOfAllMedia(MediaType mediaType, List<IMixedMediaItem> list, IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		try {
			return local_updateListOfAllMedia(mediaType, list, sort, direction, hideMissing);
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
//	Media adders and removers.
	
	@Override
	public boolean addFile (MediaType mediaType, File file) throws DbException {
		try {
			return local_addTrack(mediaType, file.getAbsolutePath(), file.lastModified());
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public boolean addFile (MediaType mediaType, String filepath, long lastModified) throws DbException {
		try {
			return local_addTrack(mediaType, filepath, lastModified);
		} catch (Exception e) {
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
	public void setHashcode (String sfile, long hashcode) throws DbException {
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
//	MediaTrack setters.
	
	@Override
	public void incTrackPlayed (String sfile) throws DbException {
		try {
			local_incTrackStartCnt(sfile, 1);
			local_setDateLastPlayed(sfile, new Date());
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
//	Unwanted methods.
	
	@Override
	public boolean addFile(File file) throws DbException {
		throw new IllegalArgumentException("Do not use this method.");
	}
	
	@Override
	public boolean addFile(String filepath, long lastModified) throws DbException {
		throw new IllegalArgumentException("Do not use this method.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
