package net.sparktank.morrigan.model.media.impl;

import java.util.Date;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaList;
import net.sparktank.morrigan.model.pictures.MediaPictureListHelper;
import net.sparktank.morrigan.model.tracks.MediaTrackListHelper;
import net.sparktank.sqlitewrapper.DbException;

public abstract class AbstractMixedMediaDb extends MediaItemDb<MixedMediaSqliteLayer, IMixedMediaItem> implements IMixedMediaList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected AbstractMixedMediaDb (String libraryName, MixedMediaSqliteLayer dbLayer) {
		super(libraryName, dbLayer);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected IMixedMediaItem getNewT(String filePath) {
		return new MixedMediaItem(filePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void incTrackStartCnt (IMixedMediaItem track, long n) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track, n);
		try {
			this.getDbLayer().incTrackStartCnt(track.getFilepath(), n);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void incTrackEndCnt (IMixedMediaItem track, long n) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track, n);
		try {
			this.getDbLayer().incTrackEndCnt(track.getFilepath(), n);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setTrackDateLastPlayed (IMixedMediaItem track, Date date) throws MorriganException {
		MediaTrackListHelper.setDateLastPlayed(this, track, date);
		try {
			this.getDbLayer().setDateLastPlayed(track.getFilepath(), date);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void incTrackStartCnt(IMixedMediaItem track) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track);
		try {
			this.getDbLayer().incTrackPlayed(track.getFilepath());
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void incTrackEndCnt(IMixedMediaItem track) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track);
		try {
			this.getDbLayer().incTrackFinished(track.getFilepath());
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setTrackDuration(IMixedMediaItem track, int duration) throws MorriganException {
		MediaTrackListHelper.setTrackDuration(this, track, duration);
		try {
			this.getDbLayer().setTrackDuration(track.getFilepath(), duration);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}

	@Override
	public void setPictureWidthAndHeight(IMixedMediaItem item, int width, int height) throws MorriganException {
		MediaPictureListHelper.setPictureWidthAndHeight(this, item, width, height);
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
