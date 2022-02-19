package com.vaguehope.morrigan.model.media.internal;

import java.util.Date;
import java.util.List;

import com.vaguehope.morrigan.model.media.DirtyState;
import com.vaguehope.morrigan.model.media.DurationData;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.IMediaTrack;

public final class MediaTrackListHelper {

	private MediaTrackListHelper () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void incTrackStartCnt (IMediaItemList<?> mtl, IMediaTrack track, long n) {
		track.setStartCount(track.getStartCount() + n);
		mtl.getChangeEventCaller().mediaItemsUpdated(track);
		mtl.setDirtyState(DirtyState.METADATA);
	}

	public static void incTrackEndCnt (IMediaItemList<?> mtl, IMediaTrack track, long n) {
		track.setEndCount(track.getEndCount() + n);
		mtl.getChangeEventCaller().mediaItemsUpdated(track);
		mtl.setDirtyState(DirtyState.METADATA);
	}

	public static void setTrackStartCnt (IMediaItemList<?> mtl, IMediaTrack track, long n) {
		track.setStartCount(n);
		mtl.getChangeEventCaller().mediaItemsUpdated(track);
		mtl.setDirtyState(DirtyState.METADATA);
	}

	public static void setTrackEndCnt (IMediaItemList<?> mtl, IMediaTrack track, long n) {
		track.setEndCount(n);
		mtl.getChangeEventCaller().mediaItemsUpdated(track);
		mtl.setDirtyState(DirtyState.METADATA);
	}

	public static void incTrackStartCnt (IMediaItemList<?> mtl, IMediaTrack track) {
		track.setStartCount(track.getStartCount() + 1);
		track.setDateLastPlayed(new Date());
		mtl.getChangeEventCaller().mediaItemsUpdated(track);
		mtl.setDirtyState(DirtyState.METADATA);
	}

	public static void incTrackEndCnt (IMediaItemList<?> mtl, IMediaTrack track) {
		track.setEndCount(track.getEndCount() + 1);
		mtl.getChangeEventCaller().mediaItemsUpdated(track);
		mtl.setDirtyState(DirtyState.METADATA);
	}

	public static void setTrackDuration (IMediaItemList<?> mtl, IMediaTrack track, int duration) {
		track.setDuration(duration);
		mtl.getChangeEventCaller().mediaItemsUpdated(track);
		mtl.setDirtyState(DirtyState.METADATA);
	}

	public static void setDateLastPlayed (IMediaItemList<?> mtl, IMediaTrack track, Date date) {
		track.setDateLastPlayed(date);
		mtl.getChangeEventCaller().mediaItemsUpdated(track);
		mtl.setDirtyState(DirtyState.METADATA);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static DurationData getTotalDuration (List<? extends IMediaTrack> mediaTracks) {
		boolean complete = true;
		long duration = 0;

		for (IMediaTrack mt : mediaTracks) {
			if (mt.getDuration() > 0) {
				duration = duration + mt.getDuration();
			}
			else {
				complete = false;
			}
		}
		return new DurationDataImpl(duration, complete);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
