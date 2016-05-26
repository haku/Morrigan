package com.vaguehope.morrigan.model.media;

import java.util.Date;

import com.vaguehope.morrigan.model.exceptions.MorriganException;

public interface IMediaTrackList<T extends IMediaTrack> extends IMediaItemList<T> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Adds n.
	 */
	void incTrackStartCnt (IMediaTrack item, long n) throws MorriganException;
	/**
	 * Adds 1 and sets last played date.
	 */
	void incTrackStartCnt (IMediaTrack item) throws MorriganException;
	void setTrackStartCnt (IMediaTrack item, long n) throws MorriganException;

	/**
	 * Adds n.
	 */
	void incTrackEndCnt (IMediaTrack item, long n) throws MorriganException;
	/**
	 * Adds 1.
	 */
	void incTrackEndCnt (IMediaTrack item) throws MorriganException;
	void setTrackEndCnt (IMediaTrack item, long n) throws MorriganException;

	void setTrackDuration (IMediaTrack item, int duration) throws MorriganException;
	void setTrackDateLastPlayed (IMediaTrack item, Date date) throws MorriganException;

	DurationData getTotalDuration ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
