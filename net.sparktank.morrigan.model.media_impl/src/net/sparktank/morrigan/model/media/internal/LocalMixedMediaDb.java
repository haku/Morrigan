package net.sparktank.morrigan.model.media.internal;

import net.sparktank.morrigan.model.factory.RecyclingFactory;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaStorageLayer;
import net.sparktank.sqlitewrapper.DbException;

public class LocalMixedMediaDb extends AbstractMixedMediaDb<ILocalMixedMediaDb> implements ILocalMixedMediaDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factories.
	
	public static class LocalMixedMediaDbFactory extends RecyclingFactory<ILocalMixedMediaDb, String, String, DbException> {
		
		protected LocalMixedMediaDbFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(ILocalMixedMediaDb product) {
//			System.out.println("Found '" + product.getDbPath() + "' in cache.");
			return true;
		}
		
		@Override
		protected ILocalMixedMediaDb makeNewProduct(String material) throws DbException {
//			System.out.println("Making object instance '" + material + "'...");
			return new LocalMixedMediaDb(LocalMixedMediaDbHelper.getMmdbTitle(material), MixedMediaSqliteLayer.FACTORY.manufacture(material), null);
		}
		
		@Override
		protected ILocalMixedMediaDb makeNewProduct(String material, String config) throws DbException {
			return new LocalMixedMediaDb(LocalMixedMediaDbHelper.getMmdbTitle(material), MixedMediaSqliteLayer.FACTORY.manufacture(material), config);
		}
		
	}
	
	public static final LocalMixedMediaDbFactory LOCAL_MMDB_FACTORY = new LocalMixedMediaDbFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "LOCALMMDB";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected LocalMixedMediaDb (String libraryName, IMixedMediaStorageLayer<IMixedMediaItem> dbLayer, String searchTerm) {
		super(libraryName, dbLayer, searchTerm);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getType() {
		return TYPE;
	}
	
	@SuppressWarnings("boxing")
	@Override
	public LocalMixedMediaDb getTransactionalClone() throws DbException {
		return new LocalMixedMediaDb(LocalMixedMediaDbHelper.getMmdbTitle(getDbPath()),
				MixedMediaSqliteLayer.FACTORY.manufacture(getDbPath(), false, true),
				this.getEscapedSearchTerm());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
