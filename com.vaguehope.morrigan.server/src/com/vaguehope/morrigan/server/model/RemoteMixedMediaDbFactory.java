package com.vaguehope.morrigan.server.model;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.factory.RecyclingFactory;
import com.vaguehope.morrigan.model.media.IMixedMediaStorageLayer;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDbConfig;
import com.vaguehope.morrigan.model.media.internal.db.mmdb.MixedMediaSqliteLayerFactory;
import com.vaguehope.sqlitewrapper.DbException;

public final class RemoteMixedMediaDbFactory {

	private RemoteMixedMediaDbFactory () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final InnerFactory INSTANCE = new InnerFactory();

	private static class InnerFactory extends RecyclingFactory<IRemoteMixedMediaDb, MediaItemDbConfig, RemoteHostDetails, MorriganException> {

		public InnerFactory () {
			super(true);
		}

		@Override
		protected boolean isValidProduct (IRemoteMixedMediaDb product) {
			return true;
		}

		@Override
		protected IRemoteMixedMediaDb makeNewProduct (MediaItemDbConfig material) throws MorriganException {
			return makeNewProduct(material, null);
		}

		@Override
		protected IRemoteMixedMediaDb makeNewProduct (MediaItemDbConfig material, RemoteHostDetails config) throws MorriganException {
			IRemoteMixedMediaDb ret = null;
			if (config != null) {
				try {
					ret = new RemoteMixedMediaDb(
							RemoteMixedMediaDbHelper.getRemoteMmdbTitle(material),
							material,
							config,
							MixedMediaSqliteLayerFactory.getAutocommit(material.getFilePath()));
				}
				catch (DbException e) {
					throw new MorriganException(e);
				}
			}
			else {
				try {
					ret = new RemoteMixedMediaDb(
							RemoteMixedMediaDbHelper.getRemoteMmdbTitle(material),
							material,
							MixedMediaSqliteLayerFactory.getAutocommit(material.getFilePath()));
				}
				catch (DbException e) {
					throw new MorriganException(e);
				}
			}
			return ret;
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static IRemoteMixedMediaDb getNew (String fullFilePath, RemoteHostDetails details) throws MorriganException {
		MediaItemDbConfig config = new MediaItemDbConfig(fullFilePath, null);
		return INSTANCE.manufacture(config, details);
	}

	public static IRemoteMixedMediaDb getExisting (String fullFilePath) throws MorriganException {
		MediaItemDbConfig config = new MediaItemDbConfig(fullFilePath, null);
		return INSTANCE.manufacture(config);
	}

	public static IRemoteMixedMediaDb getExisting (String fullFilePath, String filter) throws MorriganException {
		MediaItemDbConfig config = new MediaItemDbConfig(fullFilePath, filter);
		return INSTANCE.manufacture(config);
	}

	public static IRemoteMixedMediaDb getExistingBySerial (String serial) throws MorriganException {
		MediaItemDbConfig config = new MediaItemDbConfig(serial);
		return INSTANCE.manufacture(config);
	}

	public static IRemoteMixedMediaDb getTransactionalClone (IRemoteMixedMediaDb rmmdb) throws DbException {
		String title = RemoteMixedMediaDbHelper.getRemoteMmdbTitle(rmmdb.getDbPath());
		MediaItemDbConfig config = new MediaItemDbConfig(rmmdb.getDbPath(), null);
		IMixedMediaStorageLayer storage = MixedMediaSqliteLayerFactory.getTransactional(rmmdb.getDbPath());
		RemoteHostDetails details = new RemoteHostDetails(rmmdb.getUrl(), rmmdb.getPass());
		return new RemoteMixedMediaDb(title, config, details, storage);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
