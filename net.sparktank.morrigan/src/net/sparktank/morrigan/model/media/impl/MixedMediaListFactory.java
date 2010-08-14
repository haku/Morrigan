package net.sparktank.morrigan.model.media.impl;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.tracks.library.local.LocalLibraryHelper;
import net.sparktank.sqlitewrapper.DbException;


public class MixedMediaListFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static class LocalMixedMediaDbFactory extends RecyclingFactory<LocalMixedMediaDb, String, Void, DbException> {
		
		protected LocalMixedMediaDbFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(LocalMixedMediaDb product) {
			System.out.println("Found '" + product.getDbPath() + "' in cache.");
			return true;
		}
		
		@Override
		protected LocalMixedMediaDb makeNewProduct(String material) throws DbException {
			System.out.println("Making object instance '" + material + "'...");
			/*
			 * TODO replace LocalLibraryHelper with MixedMediaDbHelper.
			 */
			return new LocalMixedMediaDb(LocalLibraryHelper.getLibraryTitle(material), MixedMediaSqliteLayer.FACTORY.manufacture(material));
		}
		
	}
	
	public static final LocalMixedMediaDbFactory LOCAL_MMDB_FACTORY = new LocalMixedMediaDbFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
