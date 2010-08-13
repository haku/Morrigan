package net.sparktank.morrigan.model.media;

import java.util.Date;

import net.sparktank.morrigan.exceptions.MorriganException;

public interface IMediaTrackList<T extends IMediaTrack> extends IMediaItemList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public class DurationData {
		public long duration;
		public boolean complete;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void incTrackStartCnt (T item, long n) throws MorriganException;
	public void incTrackStartCnt (T item) throws MorriganException;
	public void incTrackEndCnt (T item, long n) throws MorriganException;
	public void incTrackEndCnt (T item) throws MorriganException;
	public void setTrackDuration (T item, int duration) throws MorriganException;
	public void setTrackDateLastPlayed (T item, Date date) throws MorriganException;
	
	public DurationData getTotalDuration ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
