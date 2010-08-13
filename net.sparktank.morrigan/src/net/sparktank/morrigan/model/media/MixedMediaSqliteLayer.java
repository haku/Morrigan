package net.sparktank.morrigan.model.media;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.model.DbColumn;
import net.sparktank.morrigan.model.IDbItem;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.morrigan.model.tracks.MediaTrack;
import net.sparktank.sqlitewrapper.DbException;

public class MixedMediaSqliteLayer extends MixedMediaSqliteLayerImpl implements IMixedMediaMetadataLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	protected MixedMediaSqliteLayer (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Read methods for MediaItem.
	
	@Override
	public List<MediaItem> updateListOfAllMedia(List<MediaItem> list, DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		try {
			return local_updateListOfAllMedia(list, sort, direction, hideMissing);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public List<MediaItem> getAllMedia(DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		try {
			return local_getAllMedia(sort, direction, hideMissing);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
	@Override
	public List<MediaItem> simpleSearchMedia(String term, String esc, int maxResults) throws DbException {
		try {
			return local_simpleSearch(term, esc, maxResults);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Read methods for MediaTrack.
	
	@Override
	public List<MediaTrack> updateListOfAllMediaTracks(List<MediaTrack> list, DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public List<MediaTrack> getAllMediaTracks(DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public List<MediaTrack> simpleSearchMediaTracks(String term, String esc, int maxResults)
	throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Read methods for MediaPicture.
	
	@Override
	public List<MediaPicture> updateListOfAllMediaPictures(List<MediaPicture> list, DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public List<MediaPicture> getAllMediaPictures(DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public List<MediaPicture> simpleSearchMediaPictures(String term, String esc, int maxResults) throws DbException {
		throw new RuntimeException("Not implemented yet.");
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
}
