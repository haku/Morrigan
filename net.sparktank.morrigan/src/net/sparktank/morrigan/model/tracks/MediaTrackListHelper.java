package net.sparktank.morrigan.model.tracks;

import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItemList;
import net.sparktank.morrigan.model.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.tracks.IMediaTrackList.DurationData;

public class MediaTrackListHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * @throws MorriganException  
	 */
	static public void incTrackStartCnt (MediaItemList<?> mtl, MediaTrack track, long n) throws MorriganException {
		track.setStartCount(track.getStartCount() + n);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	static public void incTrackEndCnt (MediaItemList<?> mtl, MediaTrack track, long n) throws MorriganException {
		track.setEndCount(track.getEndCount() + n);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	static public void incTrackStartCnt (MediaItemList<?> mtl, MediaTrack track) throws MorriganException {
		track.setStartCount(track.getStartCount()+1);
		track.setDateLastPlayed(new Date());
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	static public void incTrackEndCnt (MediaItemList<?> mtl, MediaTrack track) throws MorriganException {
		track.setEndCount(track.getEndCount()+1);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	static public void setTrackDuration (MediaItemList<?> mtl, MediaTrack track, int duration) throws MorriganException {
		track.setDuration(duration);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	/**
	 * @throws MorriganException  
	 */
	static public void setDateLastPlayed (MediaItemList<?> mtl, MediaTrack track, Date date) throws MorriganException {
		track.setDateLastPlayed(date);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public DurationData getTotalDuration (List<? extends MediaTrack> mediaTracks) {
		DurationData ret = new DurationData();
		ret.complete = true;
		for (MediaTrack mt : mediaTracks) {
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
