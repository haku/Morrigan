package net.sparktank.morrigan.model.tracks;

import java.util.Date;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItemList;

public abstract class MediaTrackList<T extends MediaTrack> extends MediaItemList<T> {
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
	public void incTrackStartCnt (MediaTrack track, long n) throws MorriganException {
		track.setStartCount(track.getStartCount() + n);
		setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	public void incTrackEndCnt (MediaTrack track, long n) throws MorriganException {
		track.setEndCount(track.getEndCount() + n);
		setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	public void incTrackStartCnt (MediaTrack track) throws MorriganException {
		track.setStartCount(track.getStartCount()+1);
		track.setDateLastPlayed(new Date());
		setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	public void incTrackEndCnt (MediaTrack track) throws MorriganException {
		track.setEndCount(track.getEndCount()+1);
		setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	public void setTrackDuration (MediaTrack track, int duration) throws MorriganException {
		track.setDuration(duration);
		setDirtyState(DirtyState.METADATA);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Metadata readers.
	
	static public class DurationData {
		public long duration;
		public boolean complete;
	}
	
	public DurationData getTotalDuration () {
		DurationData ret = new DurationData();
		ret.complete = true;
		for (T mt : getMediaTracks()) {
			if (mt.getDuration() > 0) {
				ret.duration = ret.duration + mt.getDuration();
			} else {
				ret.complete = false;
			}
		}
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
