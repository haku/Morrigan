package com.vaguehope.morrigan.dlna.extcd;

import java.io.File;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.FileExistance;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMediaPicture;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMixedMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.IMixedMediaStorageLayer;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.model.media.MatchMode;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaItemListChangeListener;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public abstract class EphemeralMixedMediaDb implements IMixedMediaDb {

	@Override
	public void dispose () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// DB Metadata.

	@Override
	public String getSerial () {
		return getListId();
	}

	@Override
	public String getSearchTerm () {
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Content.

	@Override
	public List<IMixedMediaItem> simpleSearch (final String term, final int maxResults) throws DbException {
		return simpleSearchMedia(getDefaultMediaType(), term, maxResults);
	}

	@Override
	public List<IMixedMediaItem> simpleSearch (final String term, final int maxResults, final IDbColumn[] sortColumns, final SortDirection[] sortDirections, final boolean includeDisabled) throws DbException {
		return simpleSearchMedia(getDefaultMediaType(), term, maxResults, sortColumns, sortDirections, includeDisabled);
	}

	@Override
	public List<IMixedMediaItem> simpleSearchMedia (final MediaType mediaType, final String term, final int maxResults) throws DbException {
		return simpleSearchMedia(mediaType, term, maxResults, null, null, false);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Fake state.

	private MediaType defaultMediaType = MediaType.UNKNOWN;
	private IDbColumn sort = IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE;
	private SortDirection direction = SortDirection.ASC;

	@Override
	public void setDefaultMediaType (final MediaType mediaType) throws MorriganException {
		setDefaultMediaType(mediaType, true);
	}

	@Override
	public void setDefaultMediaType (final MediaType mediaType, final boolean saveToDb) throws MorriganException {
		this.defaultMediaType = mediaType;
	}

	@Override
	public MediaType getDefaultMediaType () {
		return this.defaultMediaType;
	}

	@Override
	public IDbColumn getSort () {
		return this.sort;
	}

	@Override
	public SortDirection getSortDirection () {
		return this.direction;
	}

	@Override
	public void setSort (final IDbColumn sort, final SortDirection direction) throws MorriganException {
		this.sort = sort;
		this.direction = direction;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Getters that return nothing.

	@Override
	public DirtyState getDirtyState () {
		return DirtyState.CLEAN;
	}

	@Override
	public void setDirtyState (final DirtyState state) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public int getCount () {
		return 0;
	}

	@Override
	public List<IMixedMediaItem> getMediaItems () {
		return Collections.emptyList();
	}

	@Override
	public List<IMixedMediaItem> getAllDbEntries () throws DbException {
		return Collections.emptyList();
	}

	@Override
	public FileExistance hasFile (final File file) throws MorriganException, DbException {
		return FileExistance.UNKNOWN;
	}

	@Override
	public long getDurationOfLastRead () {
		return 0L;
	}

	@Override
	public DurationData getTotalDuration () {
		return new DurationData() {
			@Override
			public boolean isComplete () {
				return false;
			}

			@Override
			public long getDuration () {
				return 0L;
			}
		};
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Not supported - but possible to implement.

	@Override
	public void setHideMissing(final boolean v) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void copyItemFile (final IMixedMediaItem item, final OutputStream os) throws MorriganException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public File copyItemFile (final IMixedMediaItem item, final File targetDirectory) throws MorriganException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public List<MediaTagClassification> getTagClassifications () throws MorriganException {
		return Collections.emptyList();
	}

	@Override
	public void addTagClassification (final String classificationName) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public MediaTagClassification getTagClassification (final String classificationName) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public List<MediaTag> getTopTags (final int countLimit) throws MorriganException {
		return Collections.emptyList();
	}

	@Override
	public Map<String, MediaTag> tagSearch (final String query, final MatchMode mode, final int resLimit) throws MorriganException {
		return Collections.emptyMap();
	}

	@Override
	public boolean hasTagsIncludingDeleted (final IDbItem item) throws MorriganException {
		return false;
	}

	@Override
	public boolean hasTag (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc) throws MorriganException {
		return false;
	}

	@Override
	public List<MediaTag> getTags (final IDbItem item) throws MorriganException {
		return Collections.emptyList();
	}

	@Override
	public List<MediaTag> getTagsIncludingDeleted (final IDbItem item) throws MorriganException {
		return Collections.emptyList();
	}

	@Override
	public ItemTags readTags (final IDbItem item) throws MorriganException {
		return ItemTags.EMPTY;
	}

	@Override
	public void addTag (final IDbItem item, final String tag, final MediaTagType type, final MediaTagClassification mtc) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void addTag (final IDbItem item, final String tag, final MediaTagType type, final String mtc) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void addTag (final IDbItem item, final String tag, final MediaTagType type, final String mtc, final Date modified, final boolean deleted) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void moveTags (final IDbItem fromItem, final IDbItem toItem) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeTag (final MediaTag mt) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void clearTags (final IDbItem item) throws MorriganException {
		// NOOP.
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Not supported - ignore.

	@Override
	public void addChangeEventListener (final MediaItemListChangeListener listener) {}

	@Override
	public void removeChangeEventListener (final MediaItemListChangeListener listener) {}

	@Override
	public void registerSortChangeListener (final IMediaItemDb.SortChangeListener scl) {}

	@Override
	public void unregisterSortChangeListener (final IMediaItemDb.SortChangeListener scl) {}

	@Override
	public void incTrackStartCnt (final IMediaTrack item, final long n) throws MorriganException {}

	@Override
	public void incTrackStartCnt (final IMediaTrack item) throws MorriganException {}

	@Override
	public void incTrackEndCnt (final IMediaTrack item, final long n) throws MorriganException {}

	@Override
	public void incTrackEndCnt (final IMediaTrack item) throws MorriganException {}

	@Override
	public void setTrackDuration (final IMediaTrack item, final int duration) throws MorriganException {}

	@Override
	public void setTrackDateLastPlayed (final IMediaTrack item, final Date date) throws MorriganException {}

	@Override
	public void read () throws MorriganException {}

	@Override
	public void forceRead () throws MorriganException {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Not supported - throw.

	@Override
	public MediaItemListChangeListener getChangeEventCaller () {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void addItem (final IMixedMediaItem item) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public IMixedMediaItem addFile (final File file) throws MorriganException, DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public List<IMixedMediaItem> addFiles (final List<File> files) throws MorriganException, DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeItem (final IMixedMediaItem item) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public IMixedMediaItem getByFile (final File file) throws DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public IMixedMediaItem getByMd5 (final BigInteger md5) throws DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemMediaType (final IMixedMediaItem item, final MediaType newType) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setTrackStartCnt (final IMediaTrack item, final long n) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setTrackEndCnt (final IMediaTrack item, final long n) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemDateAdded (final IMixedMediaItem item, final Date date) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemMd5 (final IMixedMediaItem item, final BigInteger md5) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemSha1(final IMixedMediaItem item, final BigInteger sha1) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemDateLastModified (final IMixedMediaItem item, final Date date) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemEnabled (final IMixedMediaItem item, final boolean value) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemEnabled (final IMixedMediaItem item, final boolean value, final Date lastModified) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemMissing (final IMixedMediaItem item, final boolean value) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setRemoteLocation (final IMixedMediaItem track, final String remoteLocation) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void persistTrackData (final IMixedMediaItem track) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public IMixedMediaItem updateItem (final IMixedMediaItem item) throws MorriganException, DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public MediaAlbum createAlbum (final String name) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public MediaAlbum getAlbum (final String name) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeAlbum (final MediaAlbum album) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Collection<MediaAlbum> getAlbums () throws MorriganException {
		return Collections.emptyList();
	}

	@Override
	public Collection<IMixedMediaItem> getAlbumItems (final MediaAlbum album) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void addToAlbum (final MediaAlbum album, final IDbItem item) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeFromAlbum (final MediaAlbum album, final IDbItem item) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public int removeFromAllAlbums (final IDbItem item) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setPictureWidthAndHeight (final IMediaPicture item, final int width, final int height) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void commitOrRollback () throws DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void rollback () throws DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public String getDbPath () {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public IMixedMediaStorageLayer getDbLayer () {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public List<String> getSources () throws MorriganException {
		return Collections.emptyList();
	}

	@Override
	public void addSource (final String source) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeSource (final String source) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void addRemote (final String name, final URI uri) throws DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void rmRemote (final String name) throws DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public URI getRemote (final String name) throws DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Map<String, URI> getRemotes () throws DbException {
		return Collections.emptyMap();
	}

	@Override
	public boolean isMarkedAsUnreadable (final IMixedMediaItem mi) throws MorriganException {
		return false;
	}

	@Override
	public void markAsUnreadabled (final IMixedMediaItem mi) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void beginBulkUpdate () {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void completeBulkUpdate (final boolean thereWereErrors) throws MorriganException, DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Collection<IMixedMediaItem> getAlbumItems (final MediaType mediaType, final MediaAlbum album) throws MorriganException {
		return Collections.emptyList();
	}

	@Override
	public File findAlbumCoverArt (final MediaAlbum album) throws MorriganException {
		return null;
	}

}
