package com.vaguehope.morrigan.dlna.content;

import java.util.concurrent.TimeUnit;

import com.vaguehope.morrigan.dlna.util.Cache;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class DbHelper {

	private final MediaFactory mediaFactory;

	public DbHelper (final MediaFactory mediaFactory) {
		this.mediaFactory = mediaFactory;
	}

	private final Cache<MediaListReference, IMediaItemDb> dbCache = new Cache<>(10);

	public IMediaItemDb mediaListReferenceToDb (final MediaListReference mlr) throws DbException, MorriganException {
		final IMediaItemDb cached = this.dbCache.getFresh(mlr, 60, TimeUnit.SECONDS);
		if (cached != null) return cached;

		if (mlr.getType() == MediaListReference.MediaListType.LOCALMMDB) {
			final IMediaItemDb db = this.mediaFactory.getLocalMixedMediaDb(mlr.getIdentifier());
			db.read();
			this.dbCache.put(mlr, db);
			return db;
		}
		return null;
	}

}
