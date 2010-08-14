package net.sparktank.morrigan.model.pictures.gallery;

import net.sparktank.morrigan.model.media.impl.MediaItemDb;
import net.sparktank.morrigan.model.pictures.IMediaPictureList;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.morrigan.model.pictures.MediaPictureListHelper;

public abstract class AbstractGallery extends MediaItemDb<GallerySqliteLayer, MediaPicture> implements IMediaPictureList<MediaPicture> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -	

	protected AbstractGallery (String libraryName, GallerySqliteLayer dbLayer) {
		super(libraryName, dbLayer);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected MediaPicture getNewT(String filePath) {
		return new MediaPicture(filePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void setPictureWidthAndHeight (MediaPicture mp, int width, int height) {
		MediaPictureListHelper.setPictureWidthAndHeight(this, mp, width, height);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
