package net.sparktank.morrigan.model.media.impl;

import java.util.Date;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.morrigan.model.media.interfaces.IMediaPicture;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrackDb;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaList;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaStorageLayer;
import net.sparktank.morrigan.model.pictures.MediaPictureListHelper;
import net.sparktank.morrigan.model.tracks.MediaTrackListHelper;
import net.sparktank.sqlitewrapper.DbException;

public abstract class AbstractMixedMediaDb<H extends AbstractMixedMediaDb<H>>
		extends MediaItemDb<H, IMixedMediaStorageLayer<IMixedMediaItem>, IMixedMediaItem>
		implements IMixedMediaList<IMixedMediaItem>, IMediaTrackDb<H, IMixedMediaStorageLayer<IMixedMediaItem>, IMixedMediaItem> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected AbstractMixedMediaDb (String libraryName, IMixedMediaStorageLayer<IMixedMediaItem> dbLayer) {
		super(libraryName, dbLayer);
		
		try {
			readDefaultMediaTypeFromDb();
		} catch (DbException e) {
			e.printStackTrace();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected IMixedMediaItem getNewT(String filePath) {
		return new MixedMediaItem(filePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Default MediaType.
	
	public void setDefaultMediaType (MediaType mediaType) throws MorriganException {
		setDefaultMediaType(mediaType, true);
	}
	
	public void setDefaultMediaType (MediaType mediaType, boolean saveToDb) throws MorriganException {
		this.getDbLayer().setDefaultMediaType(mediaType);
		updateRead();
		if (saveToDb) saveDefaultMediaTypeToDbInNewThread();
	}
	
	public MediaType getDefaultMediaType () {
		return this.getDbLayer().getDefaultMediaType();
	}
	
	public static final String KEY_DEFAULTMEDIATYPE = "DEFAULTMEDIATYPE";
	
	private void saveDefaultMediaTypeToDbInNewThread () {
		new Thread() {
			@Override
			public void run() {
				try {
					saveDefaultMediaTypeToDb();
				} catch (DbException e) {
					e.printStackTrace();
				}
			};
		}.run();
	}
	
	void saveDefaultMediaTypeToDb () throws DbException {
		getDbLayer().setProp(KEY_DEFAULTMEDIATYPE, String.valueOf(getDefaultMediaType().getN()));
	}
	
	private void readDefaultMediaTypeFromDb () throws DbException {
		String s = getDbLayer().getProp(KEY_DEFAULTMEDIATYPE);
		if (s != null) {
    		MediaType mt = MediaType.parseInt(Integer.parseInt(s));
    		this.getDbLayer().setDefaultMediaType(mt);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void setItemMediaType(IMixedMediaItem item, MediaType newType) throws MorriganException {
		
		if (item.getMediaType() != newType) {
			if (newType == getDefaultMediaType()) {
				// TODO add this item to the list.
			}
			else if (item.getMediaType() == getDefaultMediaType()) {
				// TODO remove item from the list.
			}
		}
		
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
	protected IDbColumn parseColumnFromName(String name) {
		return MixedMediaSqliteLayerImpl.parseColumnFromName(name);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void persistTrackData(IMixedMediaItem item) throws DbException {
		super.persistTrackData(item);
		
		System.err.println(item.getFilepath() + "=" + item.getMediaType());
		this.getDbLayer().setItemMediaType(item.getFilepath(), item.getMediaType());
		
		this.getDbLayer().setTrackStartCnt(item.getFilepath(), item.getStartCount());
		this.getDbLayer().setTrackEndCnt(item.getFilepath(), item.getEndCount());
		this.getDbLayer().setTrackDuration(item.getFilepath(), item.getDuration());
		if (item.getDateLastPlayed() != null) this.getDbLayer().setDateLastPlayed(item.getFilepath(), item.getDateLastPlayed());
		
		this.getDbLayer().setDimensions(item.getFilepath(), item.getWidth(), item.getHeight());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
