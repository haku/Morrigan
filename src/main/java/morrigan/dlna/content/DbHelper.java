package morrigan.dlna.content;

import java.util.concurrent.TimeUnit;

import morrigan.model.exceptions.MorriganException;
import morrigan.model.media.ListRef;
import morrigan.model.media.MediaFactory;
import morrigan.model.media.MediaList;
import morrigan.model.media.ListRef.ListType;
import morrigan.sqlitewrapper.DbException;
import morrigan.util.Cache;

public class DbHelper {

	private final MediaFactory mediaFactory;

	public DbHelper (final MediaFactory mediaFactory) {
		this.mediaFactory = mediaFactory;
	}

	private final Cache<ListRef, MediaList> dbCache = new Cache<>(10);

	public MediaList mediaListReferenceToDb (final ListRef mlr) throws DbException, MorriganException {
		final MediaList cached = this.dbCache.getFresh(mlr, 60, TimeUnit.SECONDS);
		if (cached != null) return cached;

		if (mlr.getType() == ListType.LOCAL) {
			final MediaList db = this.mediaFactory.getList(mlr);
			db.read();
			this.dbCache.put(mlr, db);
			return db;
		}
		return null;
	}

}
