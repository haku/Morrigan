package net.sparktank.morrigan.model.tracks.library.local;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.tracks.library.AbstractMediaLibrary;
import net.sparktank.morrigan.model.tracks.library.LibrarySqliteLayer2;
import net.sparktank.sqlitewrapper.DbException;


public class LocalMediaLibrary extends AbstractMediaLibrary<LocalMediaLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory.
	
	public static class LocalMediaLibraryFactory extends RecyclingFactory<LocalMediaLibrary, String, Void, DbException> {
		
		protected LocalMediaLibraryFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(LocalMediaLibrary product) {
			System.out.println("Found '" + product.getDbPath() + "' in cache.");
			return true;
		}
		
		@Override
		protected LocalMediaLibrary makeNewProduct(String material) throws DbException {
			System.out.println("Making object instance '" + material + "'...");
			return new LocalMediaLibrary(LocalLibraryHelper.getLibraryTitle(material), LibrarySqliteLayer2.FACTORY.manufacture(material));
		}
		
	}
	
	public static final LocalMediaLibraryFactory FACTORY = new LocalMediaLibraryFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "LIBRARY";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	//TODO rename to "LocalLibrary".
	public LocalMediaLibrary (String libraryName, LibrarySqliteLayer2 dbLayer) {
		super(libraryName, dbLayer);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType() {
		return TYPE;
	}
	
	@SuppressWarnings("boxing")
	@Override
	public LocalMediaLibrary getTransactionalClone() throws DbException {
		return new LocalMediaLibrary(LocalLibraryHelper.getLibraryTitle(getDbPath()), LibrarySqliteLayer2.FACTORY.manufacture(getDbPath(), false, true));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
