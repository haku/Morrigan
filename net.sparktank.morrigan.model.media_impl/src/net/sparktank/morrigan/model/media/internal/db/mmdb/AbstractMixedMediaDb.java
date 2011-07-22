package net.sparktank.morrigan.model.media.internal.db.mmdb;

import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.model.db.IDbColumn;
import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.DirtyState;
import net.sparktank.morrigan.model.media.DurationData;
import net.sparktank.morrigan.model.media.IAbstractMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaPicture;
import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;
import net.sparktank.morrigan.model.media.IMixedMediaStorageLayer;
import net.sparktank.morrigan.model.media.internal.MediaPictureListHelper;
import net.sparktank.morrigan.model.media.internal.MediaTrackListHelper;
import net.sparktank.morrigan.model.media.internal.db.MediaItemDb;
import net.sparktank.sqlitewrapper.DbException;

public abstract class AbstractMixedMediaDb<H extends IAbstractMixedMediaDb<H>>
		extends MediaItemDb<H, IMixedMediaStorageLayer<IMixedMediaItem>, IMixedMediaItem>
		implements IAbstractMixedMediaDb<H> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected AbstractMixedMediaDb (String libraryName, IMixedMediaStorageLayer<IMixedMediaItem> dbLayer, String searchTerm) {
		super(libraryName, dbLayer, searchTerm);
		
		try {
			readDefaultMediaTypeFromDb();
		} catch (DbException e) {
			e.printStackTrace();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Default MediaType.
	
	@Override
	public void setDefaultMediaType (MediaType mediaType) throws MorriganException {
		setDefaultMediaType(mediaType, true);
	}
	
	@Override
	public void setDefaultMediaType (MediaType mediaType, boolean saveToDb) throws MorriganException {
		this.getDbLayer().setDefaultMediaType(mediaType);
		updateRead();
		if (saveToDb) saveDefaultMediaTypeToDbInNewThread();
	}
	
	@Override
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
	public List<IMixedMediaItem> simpleSearchMedia(MediaType mediaType, String term, int maxResults) throws DbException {
		return getDbLayer().simpleSearchMedia(mediaType, escapeSearch(term), SEARCH_ESC, maxResults);
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
		getChangeEventCaller().mediaItemsUpdated(item);
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
	public void setTrackStartCnt(IMediaTrack track, long n) throws MorriganException {
		MediaTrackListHelper.setTrackStartCnt(this, track, n);
		try {
			this.getDbLayer().setTrackStartCnt(track.getFilepath(), n);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
	@Override
	public void setTrackEndCnt(IMediaTrack track, long n) throws MorriganException {
		MediaTrackListHelper.setTrackEndCnt(this, track, n);
		try {
			this.getDbLayer().setTrackEndCnt(track.getFilepath(), n);
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
		return MixedMediaSqliteLayerInner.parseColumnFromName(name);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void persistTrackData(IMixedMediaItem item) throws MorriganException {
		super.persistTrackData(item);
		
		try {
			this.getDbLayer().setItemMediaType(item.getFilepath(), item.getMediaType());
			
			this.getDbLayer().setTrackStartCnt(item.getFilepath(), item.getStartCount());
			this.getDbLayer().setTrackEndCnt(item.getFilepath(), item.getEndCount());
			this.getDbLayer().setTrackDuration(item.getFilepath(), item.getDuration());
			if (item.getDateLastPlayed() != null) this.getDbLayer().setDateLastPlayed(item.getFilepath(), item.getDateLastPlayed());
			
			this.getDbLayer().setDimensions(item.getFilepath(), item.getWidth(), item.getHeight());
		}
		catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
