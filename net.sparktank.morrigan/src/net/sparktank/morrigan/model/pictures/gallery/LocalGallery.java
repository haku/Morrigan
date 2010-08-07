package net.sparktank.morrigan.model.pictures.gallery;

import net.sparktank.morrigan.model.MediaItemDb;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.morrigan.model.pictures.MediaPictureList;

/*
 * TODO FIXME Extract common code between this and AbstractMediaLibrary.
 */
public class LocalGallery extends MediaItemDb<MediaPictureList<MediaPicture>, GallerySqliteLayer, MediaPicture> /* TODO implements IMediaPictureList */ {
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
	
	/*
	TODO
	public void setPictureWidthAndHeight (MediaPicture mp, int width, int height) throws MorriganException {
		
	}
	 */
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
