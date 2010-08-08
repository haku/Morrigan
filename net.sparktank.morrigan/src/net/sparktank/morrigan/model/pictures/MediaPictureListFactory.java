package net.sparktank.morrigan.model.pictures;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.pictures.gallery.GallerySqliteLayer;
import net.sparktank.morrigan.model.pictures.gallery.LocalGallery;
import net.sparktank.morrigan.model.tracks.library.local.LocalLibraryHelper;
import net.sparktank.sqlitewrapper.DbException;

public class MediaPictureListFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static class LocalGalleryFactory extends RecyclingFactory<LocalGallery, String, Void, DbException> {
		
		protected LocalGalleryFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(LocalGallery product) {
			System.out.println("Found '" + product.getDbPath() + "' in cache.");
			return true;
		}
		
		@Override
		protected LocalGallery makeNewProduct(String material) throws DbException {
			System.out.println("Making object instance '" + material + "'...");
			/*
			 * TODO replace LocalLibraryHelper with GalleryHelper.
			 */
			return new LocalGallery(LocalLibraryHelper.getLibraryTitle(material), GallerySqliteLayer.FACTORY.manufacture(material));
		}
		
	}
	
	public static final LocalGalleryFactory LOCAL_GALLERY_FACTORY = new LocalGalleryFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
