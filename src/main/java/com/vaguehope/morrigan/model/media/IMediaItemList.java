package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.exceptions.MorriganException;


public interface IMediaItemList<T extends IMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	void dispose ();

	String getListId ();
	String getListName ();
	UUID getUuid ();

	String getType ();
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
	List<T> getMediaItems();

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

	void addItem (T item);
	void removeItem (T item) throws MorriganException;

	void setItemDateAdded (T item, Date date) throws MorriganException;
	void setItemHashCode (T item, BigInteger hashcode) throws MorriganException;
	void setItemDateLastModified (T item, Date date) throws MorriganException;
	void setItemEnabled (T item, boolean value) throws MorriganException;
	void setItemEnabled (T item, boolean value, Date lastModified) throws MorriganException;
	void setItemMissing (T item, boolean value) throws MorriganException;
	void setRemoteLocation (T track, String remoteLocation) throws MorriganException;
	void persistTrackData (T track) throws MorriganException;

	/**
	 * This will flush the OutputStream.
	 * This will not close the output stream.
	 */
	void copyItemFile (T item, OutputStream os) throws MorriganException;
	File copyItemFile (T item, File targetDirectory) throws MorriganException;

	List<MediaTagClassification> getTagClassifications () throws MorriganException;
	void addTagClassification (String classificationName) throws MorriganException;
	MediaTagClassification getTagClassification (String classificationName) throws MorriganException;
	List<MediaTag> getTopTags (int countLimit) throws MorriganException;
	Map<String, MediaTag> tagSearch (String prefix, int resLimit) throws MorriganException;
	boolean hasTags (IDbItem item) throws MorriganException;
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
	Collection<T> getAlbumItems (MediaAlbum album) throws MorriganException;
	/**
	 * Will have no effect if already in album.
	 */
	void addToAlbum (MediaAlbum album, IDbItem item) throws MorriganException;
	void removeFromAlbum (MediaAlbum album, IDbItem item) throws MorriganException;
	/**
	 * Returns number of items removed.
	 */
	int removeFromAllAlbums (IDbItem item) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
