package net.sparktank.morrigan.model.tracks;

import java.util.Date;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.IMediaItemList;

public interface IMediaTrackList<T extends MediaTrack> extends IMediaItemList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public class DurationData {
		public long duration;
		public boolean complete;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void incTrackStartCnt (MediaTrack track, long n) throws MorriganException;
	public void incTrackEndCnt (MediaTrack track, long n) throws MorriganException;
	public void incTrackStartCnt (MediaTrack track) throws MorriganException;
	public void incTrackEndCnt (MediaTrack track) throws MorriganException;
	public void setTrackDuration (MediaTrack track, int duration) throws MorriganException;
	public void setDateLastPlayed (MediaTrack track, Date date) throws MorriganException;
	
	public DurationData getTotalDuration ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
