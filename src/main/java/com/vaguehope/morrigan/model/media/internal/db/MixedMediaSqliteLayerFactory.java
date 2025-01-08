package com.vaguehope.morrigan.model.media.internal.db;

import com.vaguehope.morrigan.model.factory.RecyclingFactory;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class MixedMediaSqliteLayerFactory {

	private static class Cfg {
		final boolean autoCommit;
		final DefaultMediaItemFactory itemFactory;

		public Cfg(final boolean autoCommit, final DefaultMediaItemFactory itemFactory) {
			this.autoCommit = autoCommit;
			this.itemFactory = itemFactory;
		}
	}

	private static final RecyclingFactory<IMediaItemStorageLayer, String, Cfg, DbException> INSTANCE = new RecyclingFactory<>(true) {
		@Override
		protected boolean isValidProduct(final IMediaItemStorageLayer product) {
			return true;
		}

		@Override
		protected IMediaItemStorageLayer makeNewProduct(final String material, final Cfg cfg) throws DbException {
			return new MixedMediaSqliteLayer(material, cfg.autoCommit, cfg.itemFactory);
		}
	};

	public static IMediaItemStorageLayer getAutocommit(final String filepath, final DefaultMediaItemFactory itemFactory) throws DbException {
		return INSTANCE.manufacture(filepath, new Cfg(true, itemFactory), false);
	}

	public static IMediaItemStorageLayer getTransactional(final String filepath, final DefaultMediaItemFactory itemFactory) throws DbException {
		return INSTANCE.manufacture(filepath, new Cfg(false, itemFactory), true);
	}

}
