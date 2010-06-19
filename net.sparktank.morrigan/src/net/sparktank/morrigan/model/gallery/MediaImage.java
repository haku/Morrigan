package net.sparktank.morrigan.model.gallery;

import net.sparktank.morrigan.model.MediaItem;

public class MediaImage extends MediaItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaImage() {
		super();
	}
	
	public MediaImage(String filepath) {
		super(filepath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private int width = -1;
	private int height = -1;
	
	public int getWidth() {
		return width;
	}
	public boolean setWidth(int width) {
		if (this.width != width) {
			this.width = width;
			return true;
		}
		return false;
	}
	
	public int getHeight() {
		return height;
	}
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
	public boolean setFromMediaItem(MediaItem mi) {
		boolean setFromMediaItem = super.setFromMediaItem(mi);
		
		if (mi instanceof MediaImage) {
			MediaImage mli = (MediaImage) mi;
			
			boolean b = this.setWidth(mli.getWidth())
				|| this.setHeight(mli.getHeight());
			
			return b || setFromMediaItem;
		}
		else {
			return setFromMediaItem;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
