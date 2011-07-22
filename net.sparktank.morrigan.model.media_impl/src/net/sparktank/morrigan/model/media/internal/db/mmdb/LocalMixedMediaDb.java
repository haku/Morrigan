package net.sparktank.morrigan.model.media.internal.db.mmdb;

import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaStorageLayer;
import net.sparktank.morrigan.model.media.internal.db.MediaItemDbConfig;
import net.sparktank.sqlitewrapper.DbException;

public class LocalMixedMediaDb extends AbstractMixedMediaDb<ILocalMixedMediaDb> implements ILocalMixedMediaDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected LocalMixedMediaDb (String listName, MediaItemDbConfig config, IMixedMediaStorageLayer<IMixedMediaItem> dbLayer) {
		super(listName, config, dbLayer);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getType () {
		return TYPE;
	}
	
	@Override
	public ILocalMixedMediaDb getTransactionalClone () throws DbException {
		ILocalMixedMediaDb r = LocalMixedMediaDbFactory.getTransactional(getDbPath());
		return r;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
