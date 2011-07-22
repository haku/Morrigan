package net.sparktank.morrigan.model.media.internal.db.mmdb;

import net.sparktank.morrigan.model.factory.RecyclingFactory2;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.sqlitewrapper.DbException;

public class LocalMixedMediaDbFactory extends RecyclingFactory2<ILocalMixedMediaDb, LocalMixedMediaDbConfig, DbException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final LocalMixedMediaDbFactory INSTANCE = new LocalMixedMediaDbFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private LocalMixedMediaDbFactory () {
		super(true);
	}
	
	@Override
	protected boolean isValidProduct (ILocalMixedMediaDb product) {
		return true;
	}
	
	@Override
	protected ILocalMixedMediaDb makeNewProduct (LocalMixedMediaDbConfig material) throws DbException {
		ILocalMixedMediaDb r = new LocalMixedMediaDb(
				LocalMixedMediaDbHelper.getMmdbTitle(material.getFilePath()),
				MixedMediaSqliteLayerFactory.INSTANCE.manufacture(material.getFilePath()),
				material.getFilter()
				);
		return r;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Return the single main instance of the specified DB.
	 * For UI use.
	 * This does not use transactions.
	 * All changes are auto committed.
	 */
	public static ILocalMixedMediaDb getMain (String fullFilePath) throws DbException {
		LocalMixedMediaDbConfig config = new LocalMixedMediaDbConfig(fullFilePath, null);
		ILocalMixedMediaDb r = INSTANCE.manufacture(config);
		return r;
	}
	
	/**
	 * Returns a new instance of the DB.
	 * This should not be used in the UI.
	 * This instance uses transactions.
	 * Changes will not propagate.
	 * This DB will not use the same object cache as the main instance.
	 */
	public static ILocalMixedMediaDb getTransactional (String fullFilePath) throws DbException {
		ILocalMixedMediaDb r = new LocalMixedMediaDb(
				LocalMixedMediaDbHelper.getMmdbTitle(fullFilePath),
				MixedMediaSqliteLayerFactory.INSTANCE.manufacture(fullFilePath, Boolean.FALSE, true), // TODO check does not share object cache.
				null
				);
		return r;
	}
	
	/**
	 * Return a new instance of the DB with a filter set.
	 * This will use the same object cache as the main instance.
	 * For UI use.
	 * Changes to this DB will propagate to the main DB.
	 * 
	 * TODO needs to share ItemFactory with main instance.
	 * TODO check if should recycle instances?
	 * TODO initially, disable making changes?
	 */
	public static ILocalMixedMediaDb getView (String fullFilePath, String filter) throws DbException {
		if (true) throw new RuntimeException("Its not ready yet! ~desu");
		@SuppressWarnings("unused")
		LocalMixedMediaDbConfig config = new LocalMixedMediaDbConfig(fullFilePath, filter);
		ILocalMixedMediaDb r = INSTANCE.manufacture(config);
		return r;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}