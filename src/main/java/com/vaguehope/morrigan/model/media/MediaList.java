package com.vaguehope.morrigan.model.media;

import java.io.File;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.model.media.SortColumn.SortDirection;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.player.contentproxy.ContentProxy;
import com.vaguehope.morrigan.sqlitewrapper.DbException;


public interface MediaList extends List<AbstractItem> {

	void dispose ();

	ListRef getListRef();
	String getListName();
	UUID getUuid();

	DirtyState getDirtyState ();
	void setDirtyState (DirtyState state);

	/**
	 * A change event will occur every time the state might have changed.
	 */
	void addChangeEventListener (MediaListChangeListener listener);
	void removeChangeEventListener (MediaListChangeListener listener);
	MediaListChangeListener getChangeEventCaller ();

	List<MediaItem> getMediaItems();

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

	default MediaList resolveRef(ListRef ref) throws MorriganException {
		if (ref == null) return null;
		if (ref.getType() != getListRef().getType()) throw new IllegalArgumentException();
		if (canMakeView() && StringUtils.isNotBlank(ref.getSearch())) return makeView(ref.getSearch());
		if (hasNodes() && StringUtils.isNotBlank(ref.getNodeId())) return makeNode(ref.getNodeId(), null);
		return null;
	}

	default boolean hasNodes() {
		return false;
	}
	default String getNodeId() {
		throw new UnsupportedOperationException();
	}
	default MediaList makeNode(String nodeId, String title) throws MorriganException {
		throw new UnsupportedOperationException();
	}
	default List<MediaNode> getSubNodes() throws MorriganException {
		throw new UnsupportedOperationException();
	}

	default String prepairRemoteLocation(MediaItem item, ContentProxy contentProxy) {
		return item.getRemoteLocation();
	}

	default boolean canMakeView() {
		return false;
	}
	default MediaList makeView(String filter) throws MorriganException {
		throw new UnsupportedOperationException();
	}

	default boolean canSort() {
		return false;
	}
	default List<SortColumn> getSuportedSortColumns() throws MorriganException {
		throw new UnsupportedOperationException();
	}
	default SortColumn getSortColumn() {
		throw new UnsupportedOperationException();
	}
	default SortDirection getSortDirection() {
		throw new UnsupportedOperationException();
	}
	default void setSort(SortColumn column, SortDirection direction) throws MorriganException {
		throw new UnsupportedOperationException();
	}

	List<PlaybackOrder> getSupportedChooseMethods() throws MorriganException;
	PlaybackOrder getDefaultChooseMethod();
	MediaItem chooseItem(PlaybackOrder order, MediaItem previousItem) throws MorriganException;

	void addItem (MediaItem item);
	void removeItem (MediaItem item) throws MorriganException;

	void setItemDateAdded (MediaItem item, Date date) throws MorriganException;
	void setItemMd5 (MediaItem item, BigInteger md5) throws MorriganException;
	void setItemSha1 (MediaItem item, BigInteger sha1) throws MorriganException;
	void setItemDateLastModified (MediaItem item, Date date) throws MorriganException;
	void setItemEnabled (MediaItem item, boolean value) throws MorriganException;
	void setItemMissing (MediaItem item, boolean value) throws MorriganException;
	void setRemoteLocation (MediaItem track, String remoteLocation) throws MorriganException; // TODO unused?
	void persistTrackData (MediaItem track) throws MorriganException;

	/**
	 * This will flush the OutputStream.
	 * This will not close the output stream.
	 */
	void copyItemFile (MediaItem item, OutputStream os) throws MorriganException;
	File copyItemFile (MediaItem item, File targetDirectory) throws MorriganException;

	// TODO CLEANUP move tag methods that are only used by DBs to MediaDb.
	// TODO move tag methods to MediaItem?

	List<MediaTag> getTopTags (int countLimit) throws MorriganException;
	Map<String, MediaTag> tagSearch (String query, MatchMode mode, int resLimit) throws MorriganException;
	boolean hasTagsIncludingDeleted (IDbItem item) throws MorriganException;
	boolean hasTag (IDbItem item, String tag, MediaTagType type, MediaTagClassification mtc) throws MorriganException;

	List<MediaTag> getTagsIncludingDeleted (MediaItem item) throws MorriganException;

	/**
	 * A replacement for getTags and getTagsIncludingDeleted that returns a helpful collection object.
	 */
	ItemTags readTags (MediaItem item) throws MorriganException;

	void addTag (MediaItem item, String tag) throws MorriganException;
	void addTag (IDbItem item, String tag, MediaTagType type, String mtc) throws MorriganException;
	void addTag (IDbItem item, String tag, MediaTagType type, String mtc, Date modified, boolean deleted) throws MorriganException;
	void moveTags (IDbItem fromItem, IDbItem toItem) throws MorriganException;
	void removeTag (MediaItem item, MediaTag tag) throws MorriganException;
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
	Collection<MediaItem> getAlbumItems (MediaType mediaType, MediaAlbum album) throws MorriganException;
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

	default List<MediaItem> search (MediaType mediaType, String term, int maxResults) throws MorriganException {
		return search(mediaType, term, maxResults, (SortColumn[]) null, (SortDirection[]) null, false);
	}
	default List<MediaItem> search (MediaType mediaType, String term, int maxResults, SortColumn sortColumn, SortDirection sortDirection, boolean includeDisabled) throws MorriganException {
		return search(mediaType, term, maxResults, new SortColumn[] { sortColumn }, new SortDirection[] { sortDirection }, includeDisabled);
	}
	List<MediaItem> search (MediaType mediaType, String term, int maxResults, SortColumn[] sortColumns, SortDirection[] sortDirections, boolean includeDisabled) throws MorriganException;

	/**
	 * identifer (previously filepath) is anything the list identifies entries by, eg could also be an ID.
	 * has to match getByFile();
	 */
	FileExistance hasFile (String identifer) throws MorriganException, DbException;

	/**
	 * identifer (previously filepath) is anything the list identifies entries by, eg could also be an ID.
	 * has to match hasFile();
	 */
	MediaItem getByFile (String identifer) throws MorriganException;

	default boolean canGetByMd5() {
		return false;
	}
	default MediaItem getByMd5 (BigInteger md5) throws DbException {
		throw new UnsupportedOperationException();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// track

	/**
	 * Adds n.
	 */
	void incTrackStartCnt (MediaItem item, long n) throws MorriganException;
	/**
	 * Adds 1 and sets last played date.
	 */
	void incTrackStartCnt (MediaItem item) throws MorriganException;
	void setTrackStartCnt (MediaItem item, long n) throws MorriganException;

	/**
	 * Adds n.
	 */
	void incTrackEndCnt (MediaItem item, long n) throws MorriganException;
	/**
	 * Adds 1.
	 */
	void incTrackEndCnt (MediaItem item, boolean completed, long startTime) throws MorriganException;
	void setTrackEndCnt (MediaItem item, long n) throws MorriganException;

	void setTrackDuration (MediaItem item, int duration) throws MorriganException;
	void setTrackDateLastPlayed (MediaItem item, Date date) throws MorriganException;

	DurationData getTotalDuration ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// picture

	void setPictureWidthAndHeight (MediaItem item, int width, int height) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// mixed

	void setItemMimeType (MediaItem item, String newType) throws MorriganException;
	void setItemMediaType (MediaItem item, MediaType newType) throws MorriganException;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
