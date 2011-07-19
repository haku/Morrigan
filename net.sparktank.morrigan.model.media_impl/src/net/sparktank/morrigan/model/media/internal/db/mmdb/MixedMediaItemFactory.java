package net.sparktank.morrigan.model.media.internal.db.mmdb;

import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.IMixedMediaItem.MediaType;

/**
 * This object will be responsible for all caching of item instances
 * so that we don't have to have crazy stuff in other parts of the
 * model.
 * 
 * If all goes well these can then be removed.  I hope.
 * 
 * IMediaItemDb.beginBulkUpdate ()
 * IMediaItemDb.completeBulkUpdate ()
 * IMediaItemDb.updateItem ()
 * IMediaItemDb.getTransactionalClone ()
 * 
 */
public class MixedMediaItemFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public IMixedMediaItem getNewMediaItem (String filePath) {
		return new MixedMediaItem(filePath);
	}
	
	public IMixedMediaItem getNewMediaItem (MediaType type) {
		return new MixedMediaItem(type);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
