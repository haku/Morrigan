package net.sparktank.morrigan.model.media.interfaces;

import java.util.Date;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.impl.DurationData;

public interface IMediaTrackList<T extends IMediaTrack> extends IMediaItemList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void incTrackStartCnt (IMediaTrack item, long n) throws MorriganException;
	public void incTrackStartCnt (IMediaTrack item) throws MorriganException;
	public void incTrackEndCnt (IMediaTrack item, long n) throws MorriganException;
	public void incTrackEndCnt (IMediaTrack item) throws MorriganException;
	public void setTrackDuration (IMediaTrack item, int duration) throws MorriganException;
	public void setTrackDateLastPlayed (IMediaTrack item, Date date) throws MorriganException;
	
	public DurationData getTotalDuration ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
