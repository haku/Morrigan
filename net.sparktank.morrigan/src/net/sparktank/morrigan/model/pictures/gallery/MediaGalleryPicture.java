package net.sparktank.morrigan.model.pictures.gallery;

import net.sparktank.morrigan.helpers.EqualHelper;
import net.sparktank.morrigan.model.IDbItem;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.morrigan.model.tracks.library.MediaLibraryTrack;

/*
 * TODO FIXME Extract common code between this and MediaLibraryTrack.
 */
public class MediaGalleryPicture extends MediaPicture implements IDbItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	public MediaGalleryPicture() {
		super();
	}
	
	public MediaGalleryPicture(String filepath) {
		super(filepath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Attributes.
	
	private long dbRowId;
	private String remoteLocation;
	
	@Override
	public long getDbRowId() {
		return this.dbRowId;
	}
	@Override
	public boolean setDbRowId(long dbRowId) {
		/* Sqlite ROWID starts at 1, so if something tries to set it
		 * less than this, don't let them clear it.
		 * This is most likely when fetching a remote list over HTTP.
		 */
		if (dbRowId > 0 && this.dbRowId != dbRowId) {
			this.dbRowId = dbRowId;
			return true;
		}
		return false;
	}
	
	@Override
	public String getRemoteLocation() {
		return this.remoteLocation;
	}
	@Override
	public boolean setRemoteLocation(String remoteLocation) {
		if (!EqualHelper.areEqual(this.remoteLocation, remoteLocation)) {
			this.remoteLocation = remoteLocation;
			return true;
		}
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Mass setter.
	
	@Override
	public boolean setFromMediaItem(MediaItem mi) {
		boolean setFromMediaItem = super.setFromMediaItem(mi);
		
		if (mi instanceof MediaLibraryTrack) {
			MediaLibraryTrack mli = (MediaLibraryTrack) mi;
			
			boolean b = this.setDbRowId(mli.getDbRowId())
				|| this.setRemoteLocation(mli.getRemoteLocation());
			
			return b || setFromMediaItem;
		}
		
		return setFromMediaItem;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
