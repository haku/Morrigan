package net.sparktank.morrigan.model.tracks;

import java.util.Date;

import net.sparktank.morrigan.helpers.EqualHelper;
import net.sparktank.morrigan.model.MediaItem;


/**
 * A media track is something that has a temporal dimension;
 * music or video.
 */
public class MediaTrack extends MediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaTrack () {
		super();
	}
	
	public MediaTrack (String filePath) {
		super(filePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private int duration = -1;
	private long startCount = 0;
	private long endCount = 0;
	private Date dateLastPlayed = null;
	
	public int getDuration() {
		return this.duration;
	}
	public boolean setDuration(int duration) {
		if (this.duration != duration) {
			this.duration = duration;
			return true;
		}
		return false;
	}
	
	public long getStartCount() {
		return this.startCount;
	}
	public boolean setStartCount(long startCount) {
		if (this.startCount != startCount) {
			this.startCount = startCount;
			return true;
		}
		return false;
	}

	public long getEndCount() {
		return this.endCount;
	}
	public boolean setEndCount(long endCount) {
		if (this.endCount != endCount) {
			this.endCount = endCount;
			return true;
		}
		return false;
	}
	
	public Date getDateLastPlayed() {
		return this.dateLastPlayed;
	}
	public boolean setDateLastPlayed(Date dateLastPlayed) {
		if (!EqualHelper.areEqual(this.dateLastPlayed, dateLastPlayed)) {
			this.dateLastPlayed = dateLastPlayed;
			return true;
		}
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.
	
	@Override
	public boolean setFromMediaItem(MediaItem mi) {
		boolean setFromMediaItem = super.setFromMediaItem(mi);
		
		if (mi instanceof MediaTrack) {
			MediaTrack mt = (MediaTrack) mi;
			
			boolean b = this.setDuration(mt.getDuration())
				| this.setStartCount(mt.getStartCount())
				| this.setEndCount(mt.getEndCount())
				| this.setDateLastPlayed(mt.getDateLastPlayed());
			
			return b | setFromMediaItem;
		}
		
		return setFromMediaItem;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
