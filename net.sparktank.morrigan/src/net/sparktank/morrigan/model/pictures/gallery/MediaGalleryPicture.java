package net.sparktank.morrigan.model.pictures.gallery;

import net.sparktank.morrigan.model.DbItem;
import net.sparktank.morrigan.model.IDbItem;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.pictures.MediaPicture;

/*
 * The following classes are basically the same except for the class they extend.
 * I can not work out a simpler way to merge these classes.
 * 
 * MediaGalleryPicture
 * MediaLibraryTrack
 * 
 */
public class MediaGalleryPicture extends MediaPicture {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaGalleryPicture() {
		super();
	}
	
	public MediaGalleryPicture(String filepath) {
		super(filepath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IDbItem iDbItem = new DbItem();
	
	public IDbItem getIDbItem() {
		return this.iDbItem;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.
	
	@Override
	public boolean setFromMediaItem(MediaItem mi) {
		boolean setFromMediaItem = super.setFromMediaItem(mi);
		
		if (mi instanceof MediaGalleryPicture) {
			MediaGalleryPicture mgp = (MediaGalleryPicture) mi;
			
			return this.iDbItem.set(mgp.getIDbItem()) || setFromMediaItem;
		}
		
		return setFromMediaItem;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
