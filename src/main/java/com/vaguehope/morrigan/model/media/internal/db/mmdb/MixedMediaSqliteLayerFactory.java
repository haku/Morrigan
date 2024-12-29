package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import com.vaguehope.morrigan.model.factory.RecyclingFactory;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class MixedMediaSqliteLayerFactory extends RecyclingFactory<IMediaItemStorageLayer, String, Boolean, DbException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final MixedMediaSqliteLayerFactory INSTANCE = new MixedMediaSqliteLayerFactory();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private MixedMediaSqliteLayerFactory () {
		super(true);
	}

	@Override
	protected boolean isValidProduct (final IMediaItemStorageLayer product) {
		return true;
	}

	@Override
	protected IMediaItemStorageLayer makeNewProduct (final String material) throws DbException {
		return new MixedMediaSqliteLayerOuter(material, true, getItemFactory());
	}

	@SuppressWarnings("boxing")
	@Override
	protected IMediaItemStorageLayer makeNewProduct (final String material, final Boolean config) throws DbException {
		return new MixedMediaSqliteLayerOuter(material, config, getItemFactory());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static IMediaItemStorageLayer getAutocommit (final String filepath) throws DbException {
		IMediaItemStorageLayer l = INSTANCE.manufacture(filepath);
		return l;
	}

	public static IMediaItemStorageLayer getTransactional (final String filepath) throws DbException {
		IMediaItemStorageLayer l = INSTANCE.manufacture(filepath, Boolean.FALSE, true);
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
