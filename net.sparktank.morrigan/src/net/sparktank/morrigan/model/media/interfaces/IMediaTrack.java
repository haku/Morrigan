package net.sparktank.morrigan.model.media.interfaces;

import java.util.Date;

public interface IMediaTrack extends IMediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public int getDuration();
	public boolean setDuration(int duration);
	
	public long getStartCount();
	public boolean setStartCount(long startCount);

	public long getEndCount();
	public boolean setEndCount(long endCount);
	
	public Date getDateLastPlayed();
	public boolean setDateLastPlayed(Date dateLastPlayed);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean setFromMediaTrack (IMediaTrack mt);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
