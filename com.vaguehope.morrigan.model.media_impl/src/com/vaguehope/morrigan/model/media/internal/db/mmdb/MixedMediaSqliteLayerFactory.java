package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import com.vaguehope.morrigan.model.factory.RecyclingFactory;
import com.vaguehope.morrigan.model.media.IMixedMediaStorageLayer;
import com.vaguehope.sqlitewrapper.DbException;

public class MixedMediaSqliteLayerFactory extends RecyclingFactory<IMixedMediaStorageLayer, String, Boolean, DbException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final MixedMediaSqliteLayerFactory INSTANCE = new MixedMediaSqliteLayerFactory();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private MixedMediaSqliteLayerFactory () {
		super(true);
	}

	@Override
	protected boolean isValidProduct (final IMixedMediaStorageLayer product) {
		return true;
	}

	@Override
	protected IMixedMediaStorageLayer makeNewProduct (final String material) throws DbException {
		return new MixedMediaSqliteLayerOuter(material, true, getItemFactory());
	}

	@SuppressWarnings("boxing")
	@Override
	protected IMixedMediaStorageLayer makeNewProduct (final String material, final Boolean config) throws DbException {
		return new MixedMediaSqliteLayerOuter(material, config, getItemFactory());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static IMixedMediaStorageLayer getAutocommit (final String filepath) throws DbException {
		IMixedMediaStorageLayer l = INSTANCE.manufacture(filepath);
		return l;
	}

	public static IMixedMediaStorageLayer getTransactional (final String filepath) throws DbException {
		IMixedMediaStorageLayer l = INSTANCE.manufacture(filepath, Boolean.FALSE, true);
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
