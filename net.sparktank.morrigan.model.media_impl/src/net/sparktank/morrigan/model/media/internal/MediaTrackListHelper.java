package net.sparktank.morrigan.model.media.internal;

import java.util.Date;
import java.util.List;

import net.sparktank.morrigan.model.media.DurationData;
import net.sparktank.morrigan.model.media.IMediaItemList;
import net.sparktank.morrigan.model.media.IMediaItemList.DirtyState;
import net.sparktank.morrigan.model.media.IMediaTrack;

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
		boolean complete = true;
		long duration = 0;
		
		for (IMediaTrack mt : mediaTracks) {
			if (mt.getDuration() > 0) {
				duration= duration + mt.getDuration();
			} else {
				complete = false;
			}
		}
		return new DurationDataImpl(duration, complete);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
