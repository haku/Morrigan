package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
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
	void addTagClassification (String classificationName) throws DbException;
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
	Collection<IMediaItem> getAlbumItems (MediaType mediaType, MediaAlbum album) throws DbException;
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

	List<IMediaItem> getAllMedia (SortColumn[] sorts, SortDirection[] directions, boolean hideMissing) throws DbException;
	List<IMediaItem> getMedia (MediaType mediaType, SortColumn[] sorts, SortDirection[] directions, boolean hideMissing) throws DbException;
	List<IMediaItem> getMedia (MediaType mediaType, SortColumn[] sorts, SortDirection[] directions, boolean hideMissing, String search) throws DbException;

	FileExistance hasFile (File file) throws DbException;
	FileExistance hasFile (String filePath) throws DbException;
	IMediaItem getByFile (File file) throws DbException;
	IMediaItem getByFile (String filePath) throws DbException;
	IMediaItem getByMd5 (BigInteger md5) throws DbException;
	List<IMediaItem> search(MediaType mediaType, String term, int maxResults, SortColumn[] columns, SortDirection[] directions, boolean includeDisabled) throws DbException;

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

	void setItemMediaType(IMediaItem item, MediaType newType) throws DbException;
	void setItemMimeType(IMediaItem item, String newType) throws DbException;
	void setDateAdded(IMediaItem item, Date date) throws DbException;
	void setMd5(IMediaItem item, BigInteger md5) throws DbException;
	void setSha1(IMediaItem item, BigInteger sha1) throws DbException;
	void setDateLastModified(IMediaItem item, Date date) throws DbException;
	void setEnabled(IMediaItem item, boolean value) throws DbException;
	void setEnabled(IMediaItem item, boolean value, Date lastModified) throws DbException;
	void setMissing(IMediaItem item, boolean value) throws DbException;
	void setRemoteLocation(IMediaItem item, String remoteLocation) throws DbException;
	void setDimensions (IMediaItem item, int width, int height) throws DbException;

	/**
	 * Inc start count by one and set last played date.
	 * @param sfile
	 * @throws DbException
	 */
	void incTrackPlayed (IMediaItem item) throws DbException;

	void incTrackFinished (IMediaItem item) throws DbException;
	void incTrackStartCnt (IMediaItem item, long n) throws DbException;
	void setTrackStartCnt (IMediaItem item, long n) throws DbException;
	void incTrackEndCnt (IMediaItem item, long n) throws DbException;
	void setTrackEndCnt (IMediaItem item, long n) throws DbException;
	void setDateLastPlayed (IMediaItem item, Date date) throws DbException;
	void setTrackDuration (IMediaItem item, int duration) throws DbException;

	IMediaItem getNewT(String filePath);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}