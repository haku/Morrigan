package net.sparktank.morrigan.model.media.internal.db.mmdb;

import net.sparktank.morrigan.model.factory.RecyclingFactory;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaStorageLayer;
import net.sparktank.sqlitewrapper.DbException;

public class MixedMediaSqliteLayerFactory extends RecyclingFactory<IMixedMediaStorageLayer<IMixedMediaItem>, String, Boolean, DbException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final MixedMediaSqliteLayerFactory FACTORY = new MixedMediaSqliteLayerFactory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	MixedMediaSqliteLayerFactory() {
		super(true);
	}
	
	@Override
	protected boolean isValidProduct(IMixedMediaStorageLayer<IMixedMediaItem> product) {
		return true;
	}
	
	@Override
	protected IMixedMediaStorageLayer<IMixedMediaItem> makeNewProduct(String material) throws DbException {
		return new MixedMediaSqliteLayerOuter(material, true);
	}
	
	@SuppressWarnings("boxing")
	@Override
	protected IMixedMediaStorageLayer<IMixedMediaItem> makeNewProduct(String material, Boolean config) throws DbException {
		return new MixedMediaSqliteLayerOuter(material, config);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}