package net.sparktank.morrigan.model.tracks.library;

import net.sparktank.morrigan.model.DbItem;
import net.sparktank.morrigan.model.IDbItem;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.pictures.gallery.MediaGalleryPicture;
import net.sparktank.morrigan.model.tracks.MediaTrack;

/*
 * The following classes are basically the same except for the class they extend.
 * I can not work out a simpler way to merge these classes.
 * 
 * MediaGalleryPicture
 * MediaLibraryTrack
 * 
 */
public class MediaLibraryTrack extends MediaTrack {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaLibraryTrack() {
		super();
	}
	
	public MediaLibraryTrack(String filepath) {
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
