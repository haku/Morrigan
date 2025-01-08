package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.sqlitewrapper.IGenericDbLayer;

public interface IMediaItemStorageLayer extends IGenericDbLayer {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void addChangeListener (IMediaItemStorageLayerChangeListener listener);
	void removeChangeListener (IMediaItemStorageLayerChangeListener listener);
	IMediaItemStorageLayerChangeListener getChangeEventCaller ();

	void setProp (String key, String value) throws DbException;
	String getProp (String key) throws DbException;
	Map<String, String> getProps() throws DbException;

	List<String> getSources () throws DbException;
	void addSource (String source) throws DbException;
	void removeSource (String source) throws DbException;

	List<MediaTagClassification> getTagClassifications () throws DbException;
	MediaTagClassification addTagClassification (String classificationName) throws DbException;
	MediaTagClassification getTagClassification (String classificationName) throws DbException;
	List<MediaTag> getTopTags (int countLimit) throws DbException;
	Map<String, MediaTag> tagSearch (String query, MatchMode mode, int resLimit) throws DbException;
	boolean hasTagsIncludingDeleted (IDbItem item) throws DbException;
	boolean hasTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws DbException;
	List<MediaTag> getTags (IDbItem item, boolean includeDelete) throws DbException;
	boolean addTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws DbException;
	boolean addTag (IDbItem item, String tag, MediaTagType type, String mtc) throws DbException;
	boolean addTag (IDbItem item, String tag, MediaTagType type, String mtc, Date modified, boolean deleted) throws DbException;
	void moveTags (IDbItem fromItem, IDbItem toItem) throws DbException;
	void removeTag (MediaTag tag) throws DbException;
	void clearTags (IDbItem item) throws DbException;

	/**
	 * Returns existing album if it already exists.
	 * Name is case-insensitive.
	 */
	MediaAlbum createAlbum (String name) throws DbException;
	/**
	 * Get album, or null if not found.
	 */
	MediaAlbum getAlbum (String name) throws DbException;
	void removeAlbum (MediaAlbum album) throws DbException;
	Collection<MediaAlbum> getAlbums () throws DbException;
	Collection<MediaItem> getAlbumItems (MediaType mediaType, MediaAlbum album) throws DbException;
	/**
	 * Will have no effect if already in album.
	 */
	void addToAlbum (MediaAlbum album, IDbItem item) throws DbException;
	void removeFromAlbum (MediaAlbum album, IDbItem item) throws DbException;
	/**
	 * Returns number of items removed.
	 */
	int removeFromAllAlbums (IDbItem item) throws DbException;

	List<IDbColumn> getMediaTblColumns ();

	List<MediaItem> getAllMedia (SortColumn[] sorts, SortDirection[] directions, boolean hideMissing) throws DbException;
	List<MediaItem> getMedia (MediaType mediaType, SortColumn[] sorts, SortDirection[] directions, boolean hideMissing) throws DbException;
	List<MediaItem> getMedia (MediaType mediaType, SortColumn[] sorts, SortDirection[] directions, boolean hideMissing, String search) throws DbException;

	FileExistance hasFile (File file) throws DbException;
	FileExistance hasFile (String filePath) throws DbException;
	MediaItem getByFile (File file) throws DbException;
	MediaItem getByFile (String filePath) throws DbException;
	MediaItem getByMd5 (BigInteger md5) throws DbException;
	List<MediaItem> search(MediaType mediaType, String term, int maxResults, SortColumn[] columns, SortDirection[] directions, boolean includeDisabled) throws DbException;

	boolean[] addFiles (MediaType mediaType, List<File> files) throws DbException;

	/**
	 *
	 * @param file
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	boolean addFile (MediaType mediaType, File file) throws DbException;

	/**
	 *
	 * @param filepath
	 * @param lastModified
	 * @return true if the file needed to be added.
	 * @throws DbException
	 */
	boolean addFile (MediaType mediaType, String filepath, long lastModified) throws DbException;

	int removeFile(String sfile) throws DbException;
	int removeFile (IDbItem dbItem) throws DbException;

	void setItemMediaType(MediaItem item, MediaType newType) throws DbException;
	void setItemMimeType(MediaItem item, String newType) throws DbException;
	void setDateAdded(MediaItem item, Date date) throws DbException;
	void setMd5(MediaItem item, BigInteger md5) throws DbException;
	void setSha1(MediaItem item, BigInteger sha1) throws DbException;
	void setDateLastModified(MediaItem item, Date date) throws DbException;
	void setEnabled(MediaItem item, boolean value) throws DbException;
	void setEnabled(MediaItem item, boolean value, Date lastModified) throws DbException;
	void setMissing(MediaItem item, boolean value) throws DbException;
	void setRemoteLocation(MediaItem item, String remoteLocation) throws DbException;
	void setDimensions (MediaItem item, int width, int height) throws DbException;

	/**
	 * Inc start count by one and set last played date.
	 * @param sfile
	 * @throws DbException
	 */
	void incTrackPlayed (MediaItem item) throws DbException;

	void incTrackFinished (MediaItem item) throws DbException;
	void incTrackStartCnt (MediaItem item, long n) throws DbException;
	void setTrackStartCnt (MediaItem item, long n) throws DbException;
	void incTrackEndCnt (MediaItem item, long n) throws DbException;
	void setTrackEndCnt (MediaItem item, long n) throws DbException;
	void setDateLastPlayed (MediaItem item, Date date) throws DbException;
	void setTrackDuration (MediaItem item, int duration) throws DbException;

	MediaItem getNewT(String filePath);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}