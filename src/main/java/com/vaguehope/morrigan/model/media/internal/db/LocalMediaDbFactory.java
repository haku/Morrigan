package com.vaguehope.morrigan.model.media.internal.db;

import com.vaguehope.morrigan.model.factory.RecyclingFactory2;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaStorageLayer;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class LocalMediaDbFactory extends RecyclingFactory2<MediaDb, MediaDbConfig, DbException> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final LocalMediaDbFactory INSTANCE = new LocalMediaDbFactory();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private LocalMediaDbFactory () {
		super(true);
	}

	@Override
	protected boolean isValidProduct (MediaDb product) {
		return true;
	}

	@Override
	protected MediaDb makeNewProduct (MediaDbConfig material) throws DbException {
		final LocalMediaDb db = new LocalMediaDb(LocalMediaDbHelper.getMmdbTitle(material), material);
		final DefaultMediaItemFactory itemFactory = new DefaultMediaItemFactory(db);
		final MediaStorageLayer dbLayer = MediaSqliteLayerFactory.getAutocommit(material.getFilePath(), itemFactory);
		db.setDbLayer(dbLayer);
		return db;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * Return the single main instance of the specified DB. For UI use. This
	 * does not use transactions. All changes are auto committed.
	 */
	public static MediaDb getMain (String fullFilePath) throws DbException {
		MediaDbConfig config = new MediaDbConfig(fullFilePath, null);
		MediaDb r = INSTANCE.manufacture(config);
		return r;
	}

	public static MediaDb getMainBySerial (String serial) throws DbException {
		MediaDbConfig config = new MediaDbConfig(serial);
		MediaDb r = INSTANCE.manufacture(config);
		return r;
	}

	/**
	 * Returns a new instance of the DB. This should not be used in the UI. This
	 * instance uses transactions. Changes will not propagate. This DB will not
	 * use the same object cache as the main instance.
	 */
	public static MediaDb getTransactional (String fullFilePath) throws DbException {
		final MediaDbConfig config = new MediaDbConfig(fullFilePath, null);
		final LocalMediaDb db = new LocalMediaDb(LocalMediaDbHelper.getMmdbTitle(config), config);
		final DefaultMediaItemFactory itemFactory = new DefaultMediaItemFactory(db);
		final MediaStorageLayer dbLayer = MediaSqliteLayerFactory.getTransactional(fullFilePath, itemFactory);
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
	public static MediaDb getView (String fullFilePath, String filter) throws DbException {
		MediaDbConfig config = new MediaDbConfig(fullFilePath, filter);
		MediaDb r = INSTANCE.manufacture(config);
		return r;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
