package net.sparktank.morrigan.model.pictures;

import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaPicture;

public class MediaPicture extends MediaItem implements IMediaPicture {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaPicture() {
		super();
	}
	
	public MediaPicture(String filepath) {
		super(filepath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private int width = -1;
	private int height = -1;
	
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
	
	@Override
	public boolean setFromMediaItem (IMediaItem mi) {
		boolean setFromMediaItem = super.setFromMediaItem(mi);
		
		if (mi instanceof MediaPicture) {
			MediaPicture mli = (MediaPicture) mi;
			
			boolean b = this.setWidth(mli.getWidth())
				| this.setHeight(mli.getHeight());
			
			return b | setFromMediaItem;
		}
		
		return setFromMediaItem;
	}
	
	@Override
	public boolean setFromMediaPicture(IMediaPicture mp) {
		boolean b =
			  this.setFromMediaItem(mp)
			| this.setWidth(mp.getWidth())
			| this.setHeight(mp.getHeight());;
		return b;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
