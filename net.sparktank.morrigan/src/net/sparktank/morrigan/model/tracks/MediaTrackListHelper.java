package net.sparktank.morrigan.model.tracks;

import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.model.MediaItemList;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.tracks.IMediaTrackList.DurationData;

public class MediaTrackListHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void incTrackStartCnt (MediaItemList<?> mtl, MediaTrack track, long n) {
		track.setStartCount(track.getStartCount() + n);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	static public void incTrackEndCnt (MediaItemList<?> mtl, MediaTrack track, long n) {
		track.setEndCount(track.getEndCount() + n);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	static public void incTrackStartCnt (MediaItemList<?> mtl, MediaTrack track) {
		track.setStartCount(track.getStartCount()+1);
		track.setDateLastPlayed(new Date());
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	static public void incTrackEndCnt (MediaItemList<?> mtl, MediaTrack track) {
		track.setEndCount(track.getEndCount()+1);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	static public void setTrackDuration (MediaItemList<?> mtl, MediaTrack track, int duration) {
		track.setDuration(duration);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	static public void setDateLastPlayed (MediaItemList<?> mtl, MediaTrack track, Date date) {
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
