package net.sparktank.morrigan.model.tracks;

import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.model.media.interfaces.IMediaItemList;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrackList.DurationData;

public class MediaTrackListHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void incTrackStartCnt (IMediaItemList<?> mtl, IMediaTrack track, long n) {
		track.setStartCount(track.getStartCount() + n);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	static public void incTrackEndCnt (IMediaItemList<?> mtl, IMediaTrack track, long n) {
		track.setEndCount(track.getEndCount() + n);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	static public void incTrackStartCnt (IMediaItemList<?> mtl, IMediaTrack track) {
		track.setStartCount(track.getStartCount()+1);
		track.setDateLastPlayed(new Date());
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	static public void incTrackEndCnt (IMediaItemList<?> mtl, IMediaTrack track) {
		track.setEndCount(track.getEndCount()+1);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	static public void setTrackDuration (IMediaItemList<?> mtl, IMediaTrack track, int duration) {
		track.setDuration(duration);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
	static public void setDateLastPlayed (IMediaItemList<?> mtl, IMediaTrack track, Date date) {
		track.setDateLastPlayed(date);
		mtl.setDirtyState(DirtyState.METADATA);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public DurationData getTotalDuration (List<? extends IMediaTrack> mediaTracks) {
		DurationData ret = new DurationData();
		ret.complete = true;
		for (IMediaTrack mt : mediaTracks) {
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
