package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import com.vaguehope.morrigan.model.factory.RecyclingFactory2;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class LocalMixedMediaDbFactory extends RecyclingFactory2<IMediaItemDb, MediaItemDbConfig, DbException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final LocalMixedMediaDbFactory INSTANCE = new LocalMixedMediaDbFactory();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private LocalMixedMediaDbFactory () {
		super(true);
	}

	@Override
	protected boolean isValidProduct (IMediaItemDb product) {
		return true;
	}

	@Override
	protected IMediaItemDb makeNewProduct (MediaItemDbConfig material) throws DbException {
		final LocalMixedMediaDb db = new LocalMixedMediaDb(LocalMixedMediaDbHelper.getMmdbTitle(material), material);
		final MixedMediaItemFactory itemFactory = new MixedMediaItemFactory(db);
		final IMediaItemStorageLayer dbLayer = MixedMediaSqliteLayerFactory.getAutocommit(material.getFilePath(), itemFactory);
		db.setDbLayer(dbLayer);
		return db;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Return the single main instance of the specified DB. For UI use. This
	 * does not use transactions. All changes are auto committed.
	 */
	public static IMediaItemDb getMain (String fullFilePath) throws DbException {
		MediaItemDbConfig config = new MediaItemDbConfig(fullFilePath, null);
		IMediaItemDb r = INSTANCE.manufacture(config);
		return r;
	}

	public static IMediaItemDb getMainBySerial (String serial) throws DbException {
		MediaItemDbConfig config = new MediaItemDbConfig(serial);
		IMediaItemDb r = INSTANCE.manufacture(config);
		return r;
	}

	/**
	 * Returns a new instance of the DB. This should not be used in the UI. This
	 * instance uses transactions. Changes will not propagate. This DB will not
	 * use the same object cache as the main instance.
	 */
	public static IMediaItemDb getTransactional (String fullFilePath) throws DbException {
		final MediaItemDbConfig config = new MediaItemDbConfig(fullFilePath, null);
		final LocalMixedMediaDb db = new LocalMixedMediaDb(LocalMixedMediaDbHelper.getMmdbTitle(config), config);
		final MixedMediaItemFactory itemFactory = new MixedMediaItemFactory(db);
		final IMediaItemStorageLayer dbLayer = MixedMediaSqliteLayerFactory.getTransactional(fullFilePath, itemFactory);
		db.setDbLayer(dbLayer);
		return db;
	}

	/**
	 * Return a new instance of the DB with a filter set. This will use the same
	 * object cache as the main instance. For UI use. Changes to this DB will
	 * propagate to the main DB.
	 *
	 * There will only be one LocalMixedMediaDb per filter string. Filter string
	 * is immutable.
	 *
	 * Since there can only be one auto-commit MixedMediaSqliteLayer per DB file
	 * and since each MixedMediaSqliteLayer will only have one
	 * MixedMediaItemFactory, all auto-commit LocalMixedMediaDb will share the
	 * same MixedMediaSqliteLayer and the same MixedMediaItemFactory.
	 *
	 */
	public static IMediaItemDb getView (String fullFilePath, String filter) throws DbException {
		MediaItemDbConfig config = new MediaItemDbConfig(fullFilePath, filter);
		IMediaItemDb r = INSTANCE.manufacture(config);
		return r;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
