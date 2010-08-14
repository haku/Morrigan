package net.sparktank.morrigan.model.pictures.gallery;

import net.sparktank.morrigan.model.media.impl.MediaItemDb;
import net.sparktank.morrigan.model.media.interfaces.IMediaPicture;
import net.sparktank.morrigan.model.media.interfaces.IMediaPictureList;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.morrigan.model.pictures.MediaPictureListHelper;

public abstract class AbstractGallery extends MediaItemDb<GallerySqliteLayer, IMediaPicture> implements IMediaPictureList<IMediaPicture> {
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
	public void setPictureWidthAndHeight (IMediaPicture mp, int width, int height) {
		MediaPictureListHelper.setPictureWidthAndHeight(this, mp, width, height);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
