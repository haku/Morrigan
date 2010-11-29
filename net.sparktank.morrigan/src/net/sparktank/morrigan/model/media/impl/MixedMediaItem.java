package net.sparktank.morrigan.model.media.impl;

import java.util.Date;

import net.sparktank.morrigan.model.helper.EqualHelper;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaPicture;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;

public class MixedMediaItem extends MediaItem implements IMixedMediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MixedMediaItem (MediaType type) {
		setMediaType(type);
		
	}
	
	public MixedMediaItem (String filePath) {
		super(filePath);
	}
	
	public MixedMediaItem (MediaType type, String filePath) {
		super(filePath);
		setMediaType(type);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
//	-  -  -  -  -  -  -  -  -
//	IMixedMediaItem
	
	private MediaType type;
	
	@Override
	public MediaType getMediaType() {
		return this.type;
	}

	@Override
	public boolean setMediaType(MediaType newType) {
		if (this.type != newType) {
			this.type = newType;
			return true;
		}
		return false;
	}
	
//	-  -  -  -  -  -  -  -  -
//	IMediaTrack.
	
	@Override
	public boolean isPlayable() {
		return (getMediaType() == MediaType.TRACK);
	}
	
	private int duration;
	private long startCount;
	private long endCount;
	private Date dateLastPlayed;
	
	@Override
	public int getDuration() {
		return this.duration;
	}
	@Override
	public boolean setDuration(int duration) {
		if (this.duration != duration) {
			this.duration = duration;
			return true;
		}
		return false;
	}
	
	@Override
	public long getStartCount() {
		return this.startCount;
	}
	@Override
	public boolean setStartCount(long startCount) {
		if (this.startCount != startCount) {
			this.startCount = startCount;
			return true;
		}
		return false;
	}

	@Override
	public long getEndCount() {
		return this.endCount;
	}
	@Override
	public boolean setEndCount(long endCount) {
		if (this.endCount != endCount) {
			this.endCount = endCount;
			return true;
		}
		return false;
	}
	
	@Override
	public Date getDateLastPlayed() {
		return this.dateLastPlayed;
	}
	@Override
	public boolean setDateLastPlayed(Date dateLastPlayed) {
		if (!EqualHelper.areEqual(this.dateLastPlayed, dateLastPlayed)) {
			this.dateLastPlayed = dateLastPlayed;
			return true;
		}
		return false;
	}
	
//	-  -  -  -  -  -  -  -  -
//	IMediaPicture.
	
	@Override
	public boolean isPicture() {
		return (getMediaType() == MediaType.PICTURE);
	}
	
	private int width;
	private int height;
	
	@Override
	public int getWidth() {
		return this.width;
	}
	@Override
	public boolean setWidth(int width) {
		if (this.width != width) {
			this.width = width;
			return true;
		}
		return false;
	}
	
	@Override
	public int getHeight() {
		return this.height;
	}
	@Override
	public boolean setHeight(int height) {
		if (this.height != height) {
			this.height = height;
			return true;
		}
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.
	
	private boolean _setFromMediaTrack (IMediaTrack mt) {
		boolean b =
			  this.setDuration(mt.getDuration())
			| this.setStartCount(mt.getStartCount())
			| this.setEndCount(mt.getEndCount())
			| this.setDateLastPlayed(mt.getDateLastPlayed());
		return b;
	}
	
	@Override
	public boolean setFromMediaTrack(IMediaTrack mt) {
		boolean b =
			  this.setFromMediaItem(mt)
			| _setFromMediaTrack(mt);
		return b;
	}
	
	private boolean _setFromMediaPicture (IMediaPicture mp) {
		boolean b =
			  this.setWidth(mp.getWidth())
			| this.setHeight(mp.getHeight());
		return b;
	}
	
	@Override
	public boolean setFromMediaPicture (IMediaPicture mp) {
		boolean b =
			  this.setFromMediaItem(mp)
			| _setFromMediaPicture(mp);
		return b;
	}
	
	@Override
	public boolean setFromMediaMixedItem (IMixedMediaItem mmi) {
		boolean b =
			  this.setFromMediaItem(mmi)
			| _setFromMediaTrack(mmi)
			| _setFromMediaPicture(mmi)
			| this.setMediaType(mmi.getMediaType())
			;
		return b;
	}
	
	@Override
	public boolean setFromMediaItem (IMediaItem mi) {
		boolean ret = super.setFromMediaItem(mi);
		
		if (mi instanceof IMixedMediaItem) {
			IMixedMediaItem mmi = (IMixedMediaItem) mi;
			
			boolean b =
				this.setMediaType(mmi.getMediaType())
				;
			
			ret = b | ret;
		}
		
		if (mi instanceof IMediaTrack) {
			IMediaTrack mt = (IMediaTrack) mi;
			
			boolean b = this.setDuration(mt.getDuration())
				| this.setStartCount(mt.getStartCount())
				| this.setEndCount(mt.getEndCount())
				| this.setDateLastPlayed(mt.getDateLastPlayed());
			
			ret = b | ret;
		}
		
		if (mi instanceof IMediaPicture) {
			IMediaPicture mli = (IMediaPicture) mi;
			
			boolean b = this.setWidth(mli.getWidth())
				| this.setHeight(mli.getHeight());
			
			ret = b | ret;
		}
		
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
