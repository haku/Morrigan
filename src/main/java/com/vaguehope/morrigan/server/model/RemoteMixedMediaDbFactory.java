package com.vaguehope.morrigan.server.model;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.factory.RecyclingFactory;
import com.vaguehope.morrigan.model.media.MediaStorageLayer;
import com.vaguehope.morrigan.model.media.RemoteMediaDb;
import com.vaguehope.morrigan.model.media.internal.db.DefaultMediaItemFactory;
import com.vaguehope.morrigan.model.media.internal.db.MediaDbConfig;
import com.vaguehope.morrigan.model.media.internal.db.MediaSqliteLayerFactory;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public final class RemoteMixedMediaDbFactory {

	private RemoteMixedMediaDbFactory () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final InnerFactory INSTANCE = new InnerFactory();

	private static class InnerFactory extends RecyclingFactory<RemoteMediaDb, MediaDbConfig, RemoteHostDetails, MorriganException> {

		public InnerFactory () {
			super(true);
		}

		@Override
		protected boolean isValidProduct (final RemoteMediaDb product) {
			return true;
		}

		@Override
		protected RemoteMediaDb makeNewProduct (final MediaDbConfig material, final RemoteHostDetails config) throws MorriganException {
			try {
				final RemoteMixedMediaDb db = new RemoteMixedMediaDb(RemoteMixedMediaDbHelper.getRemoteMmdbTitle(material), material, config);
				final DefaultMediaItemFactory itemFactory = new DefaultMediaItemFactory(db);
				final MediaStorageLayer dbLayer = MediaSqliteLayerFactory.getAutocommit(material.getFilePath(), itemFactory);
				db.setDbLayer(dbLayer);
				return db;
			}
			catch (final DbException e) {
				throw new MorriganException(e);
			}
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static RemoteMediaDb getNew (final String fullFilePath, final RemoteHostDetails details) throws MorriganException {
		final MediaDbConfig config = new MediaDbConfig(fullFilePath, null);
		return INSTANCE.manufacture(config, details);
	}

	public static RemoteMediaDb getExisting (final String fullFilePath) throws MorriganException {
		final MediaDbConfig config = new MediaDbConfig(fullFilePath, null);
		return INSTANCE.manufacture(config, null);
	}

	public static RemoteMediaDb getExisting (final String fullFilePath, final String filter) throws MorriganException {
		final MediaDbConfig config = new MediaDbConfig(fullFilePath, filter);
		return INSTANCE.manufacture(config, null);
	}

	public static RemoteMediaDb getExistingBySerial (final String serial) throws MorriganException {
		final MediaDbConfig config = new MediaDbConfig(serial);
		return INSTANCE.manufacture(config, null);
	}

	public static RemoteMediaDb getTransactionalClone (final RemoteMediaDb rmmdb) throws DbException {
		final String title = RemoteMixedMediaDbHelper.getRemoteMmdbTitle(rmmdb.getDbPath());
		final MediaDbConfig config = new MediaDbConfig(rmmdb.getDbPath(), null);
		final RemoteHostDetails details = new RemoteHostDetails(rmmdb.getUri(), rmmdb.getPass());
		final RemoteMixedMediaDb db = new RemoteMixedMediaDb(title, config, details);
		final DefaultMediaItemFactory itemFactory = new DefaultMediaItemFactory(db);
		final MediaStorageLayer dbLayer = MediaSqliteLayerFactory.getTransactional(rmmdb.getDbPath(), itemFactory);
		db.setDbLayer(dbLayer);
		return db;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
