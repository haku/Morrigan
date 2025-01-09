package com.vaguehope.morrigan.dlna.extcd;

import java.io.File;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.model.db.IDbItem;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.AbstractItem;
import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.model.media.MatchMode;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.MediaListChangeListener;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public abstract class EphemeralMediaList extends AbstractList<AbstractItem> implements MediaList {

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
	public List<PlaybackOrder> getSupportedChooseMethods() {
		return Collections.emptyList();
	}

	@Override
	public PlaybackOrder getDefaultChooseMethod() {
		return PlaybackOrder.MANUAL;
	}

	@Override
	public MediaItem chooseItem(final PlaybackOrder order, final MediaItem previousItem) throws MorriganException {
		return null;
	}

	@Override
	public void copyItemFile (final MediaItem item, final OutputStream os) throws MorriganException {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public File copyItemFile (final MediaItem item, final File targetDirectory) throws MorriganException {
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
	public void addChangeEventListener (final MediaListChangeListener listener) {}

	@Override
	public void removeChangeEventListener (final MediaListChangeListener listener) {}

	@Override
	public void incTrackStartCnt (final MediaItem item, final long n) throws MorriganException {}

	@Override
	public void incTrackStartCnt (final MediaItem item) throws MorriganException {}

	@Override
	public void incTrackEndCnt (final MediaItem item, final long n) throws MorriganException {}

	@Override
	public void incTrackEndCnt (final MediaItem item) throws MorriganException {}

	@Override
	public void setTrackDuration (final MediaItem item, final int duration) throws MorriganException {}

	@Override
	public void setTrackDateLastPlayed (final MediaItem item, final Date date) throws MorriganException {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
// Not supported - throw.

	@Override
	public MediaListChangeListener getChangeEventCaller () {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void addItem (final MediaItem item) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeItem (final MediaItem item) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public MediaItem getByMd5 (final BigInteger md5) throws DbException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemMimeType(MediaItem item, String newType) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemMediaType (final MediaItem item, final MediaType newType) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setTrackStartCnt (final MediaItem item, final long n) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setTrackEndCnt (final MediaItem item, final long n) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemDateAdded (final MediaItem item, final Date date) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemMd5 (final MediaItem item, final BigInteger md5) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemSha1(final MediaItem item, final BigInteger sha1) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemDateLastModified (final MediaItem item, final Date date) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemEnabled (final MediaItem item, final boolean value) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemEnabled (final MediaItem item, final boolean value, final Date lastModified) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setItemMissing (final MediaItem item, final boolean value) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setRemoteLocation (final MediaItem track, final String remoteLocation) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void persistTrackData (final MediaItem track) throws MorriganException {
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
	public Collection<MediaItem> getAlbumItems (final MediaType mediaType, final MediaAlbum album) throws MorriganException {
		return Collections.emptyList();
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
	public void setPictureWidthAndHeight (final MediaItem item, final int width, final int height) throws MorriganException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public File findAlbumCoverArt (final MediaAlbum album) throws MorriganException {
		return null;
	}

}
