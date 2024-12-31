package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;


public interface IMediaItemList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void dispose ();

	String getListId ();
	String getListName ();
	UUID getUuid ();

	MediaListType getType ();

	/**
	 * May contain a :, e.g. "/some/path.db:filter".
	 */
	String getSerial (); // TODO rename to something more helpful?

	String getSearchTerm ();

	DirtyState getDirtyState ();
	void setDirtyState (DirtyState state);

	/**
	 * A change event will occur every time the state might have changed.
	 */
	void addChangeEventListener (MediaItemListChangeListener listener);
	void removeChangeEventListener (MediaItemListChangeListener listener);
	MediaItemListChangeListener getChangeEventCaller ();

	/**
	 * May returns -1 if no data is available.
	 */
	int getCount ();
	List<IMediaItem> getMediaItems();

	/**
	 * This is the signal to read any source data needed.
	 * This will be called soon after the constructor and before
	 * any content is read.
	 * It may be called when no work needs doing and its
	 * up to the implemented to track this.
	 */
	void read () throws MorriganException;
	void forceRead () throws MorriganException;
	long getDurationOfLastRead();

	default boolean hasNodes() {
		return false;
	}
	default MediaNode getRootNode() throws MorriganException {
		throw new UnsupportedOperationException();
	}
	default MediaNode getNode(String id) throws MorriganException {
		throw new UnsupportedOperationException();
	}

	void addItem (IMediaItem item);
	void removeItem (IMediaItem item) throws MorriganException;

	void setItemDateAdded (IMediaItem item, Date date) throws MorriganException;
	void setItemMd5 (IMediaItem item, BigInteger md5) throws MorriganException;
	void setItemSha1 (IMediaItem item, BigInteger sha1) throws MorriganException;
	void setItemDateLastModified (IMediaItem item, Date date) throws MorriganException;
	void setItemEnabled (IMediaItem item, boolean value) throws MorriganException;
	void setItemEnabled (IMediaItem item, boolean value, Date lastModified) throws MorriganException;
	void setItemMissing (IMediaItem item, boolean value) throws MorriganException;
	void setRemoteLocation (IMediaItem track, String remoteLocation) throws MorriganException;
	void persistTrackData (IMediaItem track) throws MorriganException;

	/**
	 * This will flush the OutputStream.
	 * This will not close the output stream.
	 */
	void copyItemFile (IMediaItem item, OutputStream os) throws MorriganException;
	File copyItemFile (IMediaItem item, File targetDirectory) throws MorriganException;

	List<MediaTagClassification> getTagClassifications () throws MorriganException;
	void addTagClassification (String classificationName) throws MorriganException;
	MediaTagClassification getTagClassification (String classificationName) throws MorriganException;
	List<MediaTag> getTopTags (int countLimit) throws MorriganException;
	Map<String, MediaTag> tagSearch (String query, MatchMode mode, int resLimit) throws MorriganException;
	boolean hasTagsIncludingDeleted (IDbItem item) throws MorriganException;
	boolean hasTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws MorriganException;

	List<MediaTag> getTags (IDbItem item) throws MorriganException;
	List<MediaTag> getTagsIncludingDeleted (IDbItem item) throws MorriganException;

	/**
	 * A replacement for getTags and getTagsIncludingDeleted that returns a helpful collection object.
	 */
	ItemTags readTags (IDbItem item) throws MorriganException;

	void addTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws MorriganException;
	void addTag (IDbItem item, String tag, MediaTagType type, String mtc) throws MorriganException;
	void addTag (IDbItem item, String tag, MediaTagType type, String mtc, Date modified, boolean deleted) throws MorriganException;
	void moveTags (IDbItem fromItem, IDbItem toItem) throws MorriganException;
	void removeTag (MediaTag mt) throws MorriganException;
	void clearTags (IDbItem item) throws MorriganException;

	/**
	 * Returns existing album if it already exists.
	 * Name is case-insensitive.
	 */
	MediaAlbum createAlbum (String name) throws MorriganException;
	/**
	 * Get album, or null if not found.
	 */
	MediaAlbum getAlbum (String name) throws MorriganException;
	void removeAlbum (MediaAlbum album) throws MorriganException;
	Collection<MediaAlbum> getAlbums () throws MorriganException;
	Collection<IMediaItem> getAlbumItems (MediaType mediaType, MediaAlbum album) throws MorriganException;
	/**
	 * Will have no effect if already in album.
	 */
	void addToAlbum (MediaAlbum album, IDbItem item) throws MorriganException;
	void removeFromAlbum (MediaAlbum album, IDbItem item) throws MorriganException;
	/**
	 * Returns number of items removed.
	 */
	int removeFromAllAlbums (IDbItem item) throws MorriganException;

	/**
	 * @return File path to cover art or null.
	 */
	File findAlbumCoverArt(MediaAlbum album) throws MorriganException;

	List<IMediaItem> search (MediaType mediaType, String term, int maxResults) throws DbException;
	List<IMediaItem> search (MediaType mediaType, String term, int maxResults, IDbColumn[] sortColumns, SortDirection[] sortDirections, boolean includeDisabled) throws DbException;

	/**
	 * filepath is anything the list identifies entries by, eg could also be an ID.
	 * has to match getByFile();
	 */
	FileExistance hasFile (String filepath) throws MorriganException, DbException;

	/**
	 * filepath is anything the list identifies entries by, eg could also be an ID.
	 * has to match hasFile();
	 */
	IMediaItem getByFile (String filepath) throws DbException;

	IMediaItem getByMd5 (BigInteger md5) throws DbException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// track

	/**
	 * Adds n.
	 */
	void incTrackStartCnt (IMediaItem item, long n) throws MorriganException;
	/**
	 * Adds 1 and sets last played date.
	 */
	void incTrackStartCnt (IMediaItem item) throws MorriganException;
	void setTrackStartCnt (IMediaItem item, long n) throws MorriganException;

	/**
	 * Adds n.
	 */
	void incTrackEndCnt (IMediaItem item, long n) throws MorriganException;
	/**
	 * Adds 1.
	 */
	void incTrackEndCnt (IMediaItem item) throws MorriganException;
	void setTrackEndCnt (IMediaItem item, long n) throws MorriganException;

	void setTrackDuration (IMediaItem item, int duration) throws MorriganException;
	void setTrackDateLastPlayed (IMediaItem item, Date date) throws MorriganException;

	DurationData getTotalDuration ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// picture

	void setPictureWidthAndHeight (IMediaItem item, int width, int height) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// mixed

	void setItemMimeType (IMediaItem item, String newType) throws MorriganException;
	void setItemMediaType (IMediaItem item, MediaType newType) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
