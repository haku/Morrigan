package com.vaguehope.morrigan.server.model;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.factory.RecyclingFactory;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.internal.db.DefaultMediaItemFactory;
import com.vaguehope.morrigan.model.media.internal.db.MediaDbConfig;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.MixedMediaSqliteLayerFactory;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public final class RemoteMixedMediaDbFactory {

	private RemoteMixedMediaDbFactory () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final InnerFactory INSTANCE = new InnerFactory();

	private static class InnerFactory extends RecyclingFactory<IRemoteMixedMediaDb, MediaDbConfig, RemoteHostDetails, MorriganException> {

		public InnerFactory () {
			super(true);
		}

		@Override
		protected boolean isValidProduct (final IRemoteMixedMediaDb product) {
			return true;
		}

		@Override
		protected IRemoteMixedMediaDb makeNewProduct (final MediaDbConfig material, final RemoteHostDetails config) throws MorriganException {
			try {
				final RemoteMixedMediaDb db = new RemoteMixedMediaDb(RemoteMixedMediaDbHelper.getRemoteMmdbTitle(material), material, config);
				final DefaultMediaItemFactory itemFactory = new DefaultMediaItemFactory(db);
				final IMediaItemStorageLayer dbLayer = MixedMediaSqliteLayerFactory.getAutocommit(material.getFilePath(), itemFactory);
				db.setDbLayer(dbLayer);
				return db;
			}
			catch (final DbException e) {
				throw new MorriganException(e);
			}
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static IRemoteMixedMediaDb getNew (final String fullFilePath, final RemoteHostDetails details) throws MorriganException {
		final MediaDbConfig config = new MediaDbConfig(fullFilePath, null);
		return INSTANCE.manufacture(config, details);
	}

	public static IRemoteMixedMediaDb getExisting (final String fullFilePath) throws MorriganException {
		final MediaDbConfig config = new MediaDbConfig(fullFilePath, null);
		return INSTANCE.manufacture(config, null);
	}

	public static IRemoteMixedMediaDb getExisting (final String fullFilePath, final String filter) throws MorriganException {
		final MediaDbConfig config = new MediaDbConfig(fullFilePath, filter);
		return INSTANCE.manufacture(config, null);
	}

	public static IRemoteMixedMediaDb getExistingBySerial (final String serial) throws MorriganException {
		final MediaDbConfig config = new MediaDbConfig(serial);
		return INSTANCE.manufacture(config, null);
	}

	public static IRemoteMixedMediaDb getTransactionalClone (final IRemoteMixedMediaDb rmmdb) throws DbException {
		final String title = RemoteMixedMediaDbHelper.getRemoteMmdbTitle(rmmdb.getDbPath());
		final MediaDbConfig config = new MediaDbConfig(rmmdb.getDbPath(), null);
		final RemoteHostDetails details = new RemoteHostDetails(rmmdb.getUri(), rmmdb.getPass());
		final RemoteMixedMediaDb db = new RemoteMixedMediaDb(title, config, details);
		final DefaultMediaItemFactory itemFactory = new DefaultMediaItemFactory(db);
		final IMediaItemStorageLayer dbLayer = MixedMediaSqliteLayerFactory.getTransactional(rmmdb.getDbPath(), itemFactory);
		db.setDbLayer(dbLayer);
		return db;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
