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
	
	@Override
	public boolean addFile(MediaType mediaType, File file) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public boolean addFile(MediaType mediaType, String filepath, long lastModified) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public int removeFile(String sfile) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public int removeFile(IDbItem dbItem) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public void setDateAdded(String sfile, Date date) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public void setHashcode(String sfile, long hashcode) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public void setDateLastModified(String sfile, Date date) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public void setEnabled(String sfile, boolean value) throws DbException {
		throw new RuntimeException("Not implemented yet.");
		
	}
	
	@Override
	public void setMissing(String sfile, boolean value) throws DbException {
		throw new RuntimeException("Not implemented yet.");
		
	}
	
	@Override
	public void setRemoteLocation(String sfile, String remoteLocation) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void incTrackPlayed(String sfile) throws DbException {
		throw new RuntimeException("Not implemented yet.");
		
	}
	
	@Override
	public void incTrackFinished(String sfile) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public void incTrackStartCnt(String sfile, long n) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
	@Override
	public void setTrackStartCnt(String sfile, long n) throws DbException {
		throw new RuntimeException("Not implemented yet.");
		
	}
	
	@Override
	public void incTrackEndCnt(String sfile, long n) throws DbException {
		throw new RuntimeException("Not implemented yet.");
		
	}
	
	@Override
	public void setTrackEndCnt(String sfile, long n) throws DbException {
		throw new RuntimeException("Not implemented yet.");
		
	}
	
	@Override
	public void setDateLastPlayed(String sfile, Date date) throws DbException {
		throw new RuntimeException("Not implemented yet.");
		
	}
	
	@Override
	public void setTrackDuration(String sfile, int duration) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void setDimensions(String sfile, int width, int height) throws DbException {
		throw new RuntimeException("Not implemented yet.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
