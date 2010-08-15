package net.sparktank.morrigan.model.media.impl;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMixedMediaStorageLayer;
import net.sparktank.sqlitewrapper.DbException;

public class LocalMixedMediaDb extends AbstractMixedMediaDb<LocalMixedMediaDb> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factories.
	
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
			return new LocalMixedMediaDb(LocalMixedMediaDbHelper.getMmdbTitle(material), MixedMediaSqliteLayer.FACTORY.manufacture(material));
		}
		
	}
	
	public static final LocalMixedMediaDbFactory LOCAL_MMDB_FACTORY = new LocalMixedMediaDbFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "LOCALMMDB";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected LocalMixedMediaDb (String libraryName, IMixedMediaStorageLayer<IMixedMediaItem> dbLayer) {
		super(libraryName, dbLayer);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getType() {
		return TYPE;
	}
	
	@SuppressWarnings("boxing")
	@Override
	public LocalMixedMediaDb getTransactionalClone() throws DbException {
		System.out.println("Making transactional object instance '" + getDbPath() + "'...");
		return new LocalMixedMediaDb(LocalMixedMediaDbHelper.getMmdbTitle(getDbPath()), MixedMediaSqliteLayer.FACTORY.manufacture(getDbPath(), false, true));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
