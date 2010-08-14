package net.sparktank.morrigan.model.media.impl;

import java.util.Date;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.interfaces.IMediaPicture;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaList;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaStorageLayer;
import net.sparktank.morrigan.model.pictures.MediaPictureListHelper;
import net.sparktank.morrigan.model.tracks.MediaTrackListHelper;
import net.sparktank.sqlitewrapper.DbException;

public abstract class AbstractMixedMediaDb extends MediaItemDb<IMixedMediaStorageLayer<IMixedMediaItem>, IMixedMediaItem> implements IMixedMediaList<IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected AbstractMixedMediaDb (String libraryName, IMixedMediaStorageLayer<IMixedMediaItem> dbLayer) {
		super(libraryName, dbLayer);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected IMixedMediaItem getNewT(String filePath) {
		return new MixedMediaItem(filePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void setItemMediaType(IMixedMediaItem item, MediaType newType) throws MorriganException {
		item.setMediaType(newType);
		this.setDirtyState(DirtyState.METADATA);
		try {
			this.getDbLayer().setItemMediaType(item.getFilepath(), newType);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void incTrackStartCnt (IMediaTrack track, long n) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track, n);
		try {
			this.getDbLayer().incTrackStartCnt(track.getFilepath(), n);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void incTrackEndCnt (IMediaTrack track, long n) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track, n);
		try {
			this.getDbLayer().incTrackEndCnt(track.getFilepath(), n);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setTrackDateLastPlayed (IMediaTrack track, Date date) throws MorriganException {
		MediaTrackListHelper.setDateLastPlayed(this, track, date);
		try {
			this.getDbLayer().setDateLastPlayed(track.getFilepath(), date);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void incTrackStartCnt(IMediaTrack track) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track);
		try {
			this.getDbLayer().incTrackPlayed(track.getFilepath());
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void incTrackEndCnt(IMediaTrack track) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track);
		try {
			this.getDbLayer().incTrackFinished(track.getFilepath());
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setTrackDuration(IMediaTrack track, int duration) throws MorriganException {
		MediaTrackListHelper.setTrackDuration(this, track, duration);
		try {
			this.getDbLayer().setTrackDuration(track.getFilepath(), duration);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setPictureWidthAndHeight(IMediaPicture item, int width, int height) throws MorriganException {
		MediaPictureListHelper.setPictureWidthAndHeight(this, item, width, height);
		try {
			this.getDbLayer().setDimensions(item.getFilepath(), width, height);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public DurationData getTotalDuration() {
		return MediaTrackListHelper.getTotalDuration(this.getMediaItems());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void persistTrackData(IMixedMediaItem item) throws DbException {
		super.persistTrackData(item);
		
		this.getDbLayer().setTrackStartCnt(item.getFilepath(), item.getStartCount());
		this.getDbLayer().setTrackEndCnt(item.getFilepath(), item.getEndCount());
		this.getDbLayer().setTrackDuration(item.getFilepath(), item.getDuration());
		if (item.getDateLastPlayed() != null) this.getDbLayer().setDateLastPlayed(item.getFilepath(), item.getDateLastPlayed());
		
		this.getDbLayer().setDimensions(item.getFilepath(), item.getWidth(), item.getHeight());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
