package net.sparktank.morrigan.model.pictures.gallery;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.sqlitewrapper.DbException;

public class LocalGallery extends AbstractGallery<LocalGallery> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory.
	
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
			return new LocalGallery(LocalGalleryHelper.getGalleryTitle(material), GallerySqliteLayer.FACTORY.manufacture(material));
		}
		
	}
	
	public static final LocalGalleryFactory FACTORY = new LocalGalleryFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "LOCALGALLERY";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public LocalGallery (String libraryName, GallerySqliteLayer dbLayer) {
		super(libraryName, dbLayer);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@SuppressWarnings("boxing")
	@Override
	public LocalGallery getTransactionalClone() throws DbException {
		return new LocalGallery(LocalGalleryHelper.getGalleryTitle(getDbPath()), GallerySqliteLayer.FACTORY.manufacture(getDbPath(), false, true));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
