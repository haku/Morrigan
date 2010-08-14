package net.sparktank.morrigan.model.pictures.gallery;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.impl.MediaItemDb;
import net.sparktank.morrigan.model.media.interfaces.IMediaPicture;
import net.sparktank.morrigan.model.media.interfaces.IMediaPictureList;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.morrigan.model.pictures.MediaPictureListHelper;
import net.sparktank.sqlitewrapper.DbException;

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
	public void setPictureWidthAndHeight (IMediaPicture mp, int width, int height) throws MorriganException {
		MediaPictureListHelper.setPictureWidthAndHeight(this, mp, width, height);
		try {
			this.getDbLayer().setDimensions(mp.getFilepath(), width, height);
		} catch (DbException e) {
			throw new MorriganException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
