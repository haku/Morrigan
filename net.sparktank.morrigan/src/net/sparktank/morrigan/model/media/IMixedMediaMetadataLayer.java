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

public interface IMixedMediaMetadataLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Fixed enums - changing these requires writing more code.
	
	public static enum MediaType {
		UNKNOWN(0), TRACK(1), PICTURE(2);
		
		private final int n;
		
		MediaType(int n) {
			this.n = n;
		}
		
		public int getN() {
			return this.n;
		}
		
		static public MediaType parseInt (int n) {
			switch (n) {
				case 0: return UNKNOWN;
				case 1: return TRACK;
				case 2: return PICTURE;
				default: throw new IllegalArgumentException();
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Read methods.
	
	public List<MediaItem> updateListOfAllMedia (List<MediaItem> list, DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<MediaItem> getAllMedia (DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<MediaItem> simpleSearchMedia (String term, String esc, int maxResults) throws DbException;
	
	public List<MediaTrack> updateListOfAllMediaTracks (List<MediaTrack> list, DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<MediaTrack> getAllMediaTracks (DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<MediaTrack> simpleSearchMediaTracks (String term, String esc, int maxResults) throws DbException;
	
	public List<MediaPicture> updateListOfAllMediaPictures (List<MediaPicture> list, DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<MediaPicture> getAllMediaPictures (DbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<MediaPicture> simpleSearchMediaPictures (String term, String esc, int maxResults) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for generic MediaItem.
	
	/**
	 * 
	 * @param file
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	public boolean addFile (MediaType mediaType, File file) throws DbException;
	
	/**
	 * 
	 * @param filepath
	 * @param lastModified
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	public boolean addFile (MediaType mediaType, String filepath, long lastModified) throws DbException;
	
	public int removeFile (String sfile) throws DbException;
	public int removeFile (IDbItem dbItem) throws DbException;
	
	public void setDateAdded (String sfile, Date date) throws DbException;
	public void setHashcode (String sfile, long hashcode) throws DbException;
	public void setDateLastModified (String sfile, Date date) throws DbException;
	public void setEnabled (String sfile, boolean value) throws DbException;
	public void setMissing (String sfile, boolean value) throws DbException;
	public void setRemoteLocation (String sfile, String remoteLocation) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for MediaTrack.
	
	/**
	 * Inc start count by one and set last played date.
	 * @param sfile
	 * @throws DbException
	 */
	public void incTrackPlayed (String sfile) throws DbException;
	
	public void incTrackFinished (String sfile) throws DbException;
	public void incTrackStartCnt (String sfile, long n) throws DbException;
	public void setTrackStartCnt (String sfile, long n) throws DbException;
	public void incTrackEndCnt (String sfile, long n) throws DbException;
	public void setTrackEndCnt (String sfile, long n) throws DbException;
	public void setDateLastPlayed (String sfile, Date date) throws DbException;
	public void setTrackDuration (String sfile, int duration) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Setters for MediaPicture.
	
	public void setDimensions (String sfile, int width, int height) throws DbException;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
