package net.sparktank.morrigan.model.media.internal.db.mmdb;

import net.sparktank.morrigan.model.factory.RecyclingFactory;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaStorageLayer;
import net.sparktank.sqlitewrapper.DbException;

public class MixedMediaSqliteLayerFactory extends RecyclingFactory<IMixedMediaStorageLayer<IMixedMediaItem>, String, Boolean, DbException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final MixedMediaSqliteLayerFactory INSTANCE = new MixedMediaSqliteLayerFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MixedMediaSqliteLayerFactory() {
		super(true);
	}
	
	@Override
	protected boolean isValidProduct(IMixedMediaStorageLayer<IMixedMediaItem> product) {
		return true;
	}
	
	@Override
	protected IMixedMediaStorageLayer<IMixedMediaItem> makeNewProduct(String material) throws DbException {
		return new MixedMediaSqliteLayerOuter(material, true, getItemFactory());
	}
	
	@SuppressWarnings("boxing")
	@Override
	protected IMixedMediaStorageLayer<IMixedMediaItem> makeNewProduct(String material, Boolean config) throws DbException {
		return new MixedMediaSqliteLayerOuter(material, config, getItemFactory());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static IMixedMediaStorageLayer<IMixedMediaItem> getAutocommit (String filepath) throws DbException {
		IMixedMediaStorageLayer<IMixedMediaItem> l = INSTANCE.manufacture(filepath);
		return l;
	}
	
	public static IMixedMediaStorageLayer<IMixedMediaItem> getTransactional (String filepath) throws DbException {
		IMixedMediaStorageLayer<IMixedMediaItem> l = INSTANCE.manufacture(filepath, Boolean.FALSE, true);
		return l;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO share this between same DBs?
	 */
	private static MixedMediaItemFactory getItemFactory () {
		return new MixedMediaItemFactory();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}