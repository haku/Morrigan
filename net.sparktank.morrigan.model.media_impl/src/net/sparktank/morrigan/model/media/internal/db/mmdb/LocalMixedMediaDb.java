package net.sparktank.morrigan.model.media.internal.db.mmdb;

import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaStorageLayer;
import net.sparktank.sqlitewrapper.DbException;

public class LocalMixedMediaDb extends AbstractMixedMediaDb<ILocalMixedMediaDb> implements ILocalMixedMediaDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected LocalMixedMediaDb (String libraryName, IMixedMediaStorageLayer<IMixedMediaItem> dbLayer, String searchTerm) {
		super(libraryName, dbLayer, searchTerm);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType () {
		return TYPE;
	}
	
	@SuppressWarnings("boxing")
	@Override
	public LocalMixedMediaDb getTransactionalClone () throws DbException {
		return new LocalMixedMediaDb(LocalMixedMediaDbHelper.getMmdbTitle(getDbPath()),
				MixedMediaSqliteLayerFactory.INSTANCE.manufacture(getDbPath(), false, true),
				this.getEscapedSearchTerm());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
