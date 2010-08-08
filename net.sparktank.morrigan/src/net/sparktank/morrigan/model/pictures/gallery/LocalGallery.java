package net.sparktank.morrigan.model.pictures.gallery;

import net.sparktank.morrigan.model.MediaItemDb;
import net.sparktank.morrigan.model.pictures.IMediaPictureList;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.morrigan.model.pictures.MediaPictureListHelper;

public class LocalGallery extends MediaItemDb<IMediaPictureList<MediaPicture>, GallerySqliteLayer, MediaPicture> implements IMediaPictureList<MediaPicture> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "LOCALGALLERY";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected LocalGallery (String libraryName, GallerySqliteLayer dbLayer) {
		super(libraryName, dbLayer);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected MediaPicture getNewT(String filePath) {
		return new MediaPicture(filePath);
	}
	
	@Override
	public String getType() {
		return TYPE;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void setPictureWidthAndHeight (MediaPicture mp, int width, int height) {
		MediaPictureListHelper.setPictureWidthAndHeight(this, mp, width, height);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
