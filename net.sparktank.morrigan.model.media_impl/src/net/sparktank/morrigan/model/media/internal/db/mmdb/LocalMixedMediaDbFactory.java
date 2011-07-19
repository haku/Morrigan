package net.sparktank.morrigan.model.media.internal.db.mmdb;

import net.sparktank.morrigan.model.factory.RecyclingFactory;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.sqlitewrapper.DbException;

public class LocalMixedMediaDbFactory extends RecyclingFactory<ILocalMixedMediaDb, String, String, DbException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final LocalMixedMediaDbFactory INSTANCE = new LocalMixedMediaDbFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private LocalMixedMediaDbFactory () {
		super(true);
	}
	
	@Override
	protected boolean isValidProduct (ILocalMixedMediaDb product) {
//		System.out.println("Found '" + product.getDbPath() + "' in cache.");
		return true;
	}
	
	@Override
	protected ILocalMixedMediaDb makeNewProduct (String material) throws DbException {
//		System.out.println("Making object instance '" + material + "'...");
		return new LocalMixedMediaDb(LocalMixedMediaDbHelper.getMmdbTitle(material), MixedMediaSqliteLayerFactory.INSTANCE.manufacture(material), null);
	}
	
	@Override
	protected ILocalMixedMediaDb makeNewProduct (String material, String config) throws DbException {
		return new LocalMixedMediaDb(LocalMixedMediaDbHelper.getMmdbTitle(material), MixedMediaSqliteLayerFactory.INSTANCE.manufacture(material), config);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}