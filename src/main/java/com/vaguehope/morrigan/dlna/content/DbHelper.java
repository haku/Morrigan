package com.vaguehope.morrigan.dlna.content;

import java.util.concurrent.TimeUnit;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.Cache;

public class DbHelper {

	private final MediaFactory mediaFactory;

	public DbHelper (final MediaFactory mediaFactory) {
		this.mediaFactory = mediaFactory;
	}

	private final Cache<MediaListReference, MediaDb> dbCache = new Cache<>(10);

	public MediaDb mediaListReferenceToDb (final MediaListReference mlr) throws DbException, MorriganException {
		final MediaDb cached = this.dbCache.getFresh(mlr, 60, TimeUnit.SECONDS);
		if (cached != null) return cached;

		if (mlr.getType() == MediaListReference.MediaListType.LOCALMMDB) {
			final MediaDb db = this.mediaFactory.getLocalMixedMediaDb(mlr.getIdentifier());
			db.read();
			this.dbCache.put(mlr, db);
			return db;
		}
		return null;
	}

}
