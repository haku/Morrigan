package com.vaguehope.morrigan.model.media.internal.db;

import com.vaguehope.morrigan.model.factory.RecyclingFactory;
import com.vaguehope.morrigan.model.media.MediaStorageLayer;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class MediaSqliteLayerFactory {

	private static class Cfg {
		final boolean autoCommit;
		final DefaultMediaItemFactory itemFactory;

		public Cfg(final boolean autoCommit, final DefaultMediaItemFactory itemFactory) {
			this.autoCommit = autoCommit;
			this.itemFactory = itemFactory;
		}
	}

	private static final RecyclingFactory<MediaStorageLayer, String, Cfg, DbException> INSTANCE = new RecyclingFactory<>(true) {
		@Override
		protected boolean isValidProduct(final MediaStorageLayer product) {
			return true;
		}

		@Override
		protected MediaStorageLayer makeNewProduct(final String material, final Cfg cfg) throws DbException {
			return new MediaSqliteLayer(material, cfg.autoCommit, cfg.itemFactory);
		}
	};

	public static MediaStorageLayer getAutocommit(final String filepath, final DefaultMediaItemFactory itemFactory) throws DbException {
		return INSTANCE.manufacture(filepath, new Cfg(true, itemFactory), false);
	}

	public static MediaStorageLayer getTransactional(final String filepath, final DefaultMediaItemFactory itemFactory) throws DbException {
		return INSTANCE.manufacture(filepath, new Cfg(false, itemFactory), true);
	}

}
