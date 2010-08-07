package net.sparktank.morrigan.model.tracks;

import java.util.Date;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItemList;

@Deprecated
public abstract class MediaTrackList<T extends MediaTrack> extends MediaItemList<T> implements IMediaTrackList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected MediaTrackList(String listId, String listName) {
		super(listId, listName);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Update methods.  Use these for data that is to be persisted.
//	These methods are sub-classed where persistence is needed.
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void incTrackStartCnt (MediaTrack track, long n) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track, n);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void incTrackEndCnt (MediaTrack track, long n) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track, n);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void incTrackStartCnt (MediaTrack track) throws MorriganException {
		MediaTrackListHelper.incTrackStartCnt(this, track);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void incTrackEndCnt (MediaTrack track) throws MorriganException {
		MediaTrackListHelper.incTrackEndCnt(this, track);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void setTrackDuration (MediaTrack track, int duration) throws MorriganException {
		MediaTrackListHelper.setTrackDuration(this, track, duration);
	}
	
	/**
	 * @throws MorriganException  
	 */
	@Override
	public void setDateLastPlayed (MediaTrack track, Date date) throws MorriganException {
		MediaTrackListHelper.setDateLastPlayed(this, track, date);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Metadata readers.
	
	@Override
	public DurationData getTotalDuration () {
		return MediaTrackListHelper.getTotalDuration(this.getMediaTracks());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
