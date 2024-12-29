package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.MediaAlbum;
import com.vaguehope.morrigan.model.media.internal.CoverArtHelper;
import com.vaguehope.morrigan.model.media.internal.MediaPictureListHelper;
import com.vaguehope.morrigan.model.media.internal.MediaTrackListHelper;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDb;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public abstract class AbstractMixedMediaDb extends MediaItemDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected AbstractMixedMediaDb (final String listName, final MediaItemDbConfig config, final IMediaItemStorageLayer dbLayer) {
		super(listName, config, dbLayer);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public List<IMediaItem> simpleSearch(final MediaType mediaType, final String term, final int maxResults) throws DbException {
		return getDbLayer().simpleSearch(mediaType, term, maxResults);
	}

	@Override
	public List<IMediaItem> simpleSearch(final MediaType mediaType, final String term, final int maxResults, final IDbColumn[] sortColumns, final SortDirection[] sortDirections, final boolean includeDisabled) throws DbException {
		return getDbLayer().simpleSearch(mediaType, term, maxResults, sortColumns, sortDirections, includeDisabled);
	}

	@Override
	public Collection<IMediaItem> getAlbumItems (final MediaType mediaType, final MediaAlbum album) throws MorriganException {
		try {
			return getDbLayer().getAlbumItems(mediaType, album);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public File findAlbumCoverArt (final MediaAlbum album) throws MorriganException {
		return CoverArtHelper.findCoverArt(getAlbumItems(MediaType.PICTURE, album)); // FIXME set max result count for getAlbumItems().
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void setItemMediaType (final IMediaItem item, final MediaType newType) throws MorriganException {
		item.setMediaType(newType);
		getChangeEventCaller().mediaItemsUpdated(item);
		this.setDirtyState(DirtyState.METADATA);
		try {
			this.getDbLayer().setItemMediaType(item, newType);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void incTrackStartCnt (final IMediaItem track, final long n) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track, n);
		try {
			this.getDbLayer().incTrackStartCnt(track, n);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void incTrackEndCnt (final IMediaItem track, final long n) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track, n);
		try {
			this.getDbLayer().incTrackEndCnt(track, n);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setTrackDateLastPlayed (final IMediaItem track, final Date date) throws MorriganException {
		MediaTrackListHelper.setDateLastPlayed(this, track, date);
		try {
			this.getDbLayer().setDateLastPlayed(track, date);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void incTrackStartCnt (final IMediaItem track) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track);
		try {
			this.getDbLayer().incTrackPlayed(track);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void incTrackEndCnt (final IMediaItem track) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track);
		try {
			this.getDbLayer().incTrackFinished(track);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setTrackStartCnt (final IMediaItem track, final long n) throws MorriganException {
		MediaTrackListHelper.setTrackStartCnt(this, track, n);
		try {
			this.getDbLayer().setTrackStartCnt(track, n);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setTrackEndCnt (final IMediaItem track, final long n) throws MorriganException {
		MediaTrackListHelper.setTrackEndCnt(this, track, n);
		try {
			this.getDbLayer().setTrackEndCnt(track, n);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setTrackDuration (final IMediaItem track, final int duration) throws MorriganException {
		MediaTrackListHelper.setTrackDuration(this, track, duration);
		try {
			this.getDbLayer().setTrackDuration(track, duration);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setPictureWidthAndHeight (final IMediaItem item, final int width, final int height) throws MorriganException {
		MediaPictureListHelper.setPictureWidthAndHeight(this, item, width, height);
		try {
			this.getDbLayer().setDimensions(item, width, height);
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public DurationData getTotalDuration () {
		return MediaTrackListHelper.getTotalDuration(this.getMediaItems());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected IDbColumn parseColumnFromName (final String name) {
		return MixedMediaSqliteLayerInner.parseColumnFromName(name);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void persistTrackData (final IMediaItem item) throws MorriganException {
		super.persistTrackData(item);

		try {
			this.getDbLayer().setItemMediaType(item, item.getMediaType());

			this.getDbLayer().setTrackStartCnt(item, item.getStartCount());
			this.getDbLayer().setTrackEndCnt(item, item.getEndCount());
			this.getDbLayer().setTrackDuration(item, item.getDuration());
			if (item.getDateLastPlayed() != null) this.getDbLayer().setDateLastPlayed(item, item.getDateLastPlayed());

			this.getDbLayer().setDimensions(item, item.getWidth(), item.getHeight());
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
