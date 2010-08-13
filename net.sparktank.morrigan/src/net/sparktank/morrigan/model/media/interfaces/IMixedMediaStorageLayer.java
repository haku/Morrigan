package net.sparktank.morrigan.model.media.interfaces;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.sqlitewrapper.DbException;

public interface IMixedMediaStorageLayer<T extends IMixedMediaItem> extends IMediaItemStorageLayer<T> {
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
	
	public List<IMixedMediaItem> getAllMedia (MediaType mediaType, IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<IMixedMediaItem> updateListOfAllMedia (MediaType mediaType, List<IMixedMediaItem> list, IDbColumn sort, SortDirection direction, boolean hideMissing) throws DbException;
	public List<IMixedMediaItem> simpleSearchMedia (MediaType mediaType, String term, String esc, int maxResults) throws DbException;
	
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
