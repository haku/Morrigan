package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import com.vaguehope.morrigan.model.factory.RecyclingFactory2;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;

/**
 * 2025-01-07: this is awful, but really annoying to get rid of right now.
 * 
 * This object will be responsible for all caching of item instances
 * so that we don't have to have complex stuff in other parts of the
 * model.
 *
 * This class will be instanceateable as each instance will hold a
 * its own cache.
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

	private final RecyclingFactory2<MixedMediaItem, String, RuntimeException> factory = new RecyclingFactory2<>(true) {
		@Override
		protected boolean isValidProduct (final MixedMediaItem product) {
			return true;
		}

		@Override
		protected MixedMediaItem makeNewProduct (final String material) {
			return newItem(material);
		}
	};

	private final IMediaItemList list;

	public MixedMediaItemFactory (final IMediaItemList list) {
		this.list = list;
	}

	public IMediaItem getNewMediaItem (final String filePath) {
		if (filePath == null) return newItem(null); // We can not cache these. :(
		return this.factory.manufacture(filePath);
	}

	MixedMediaItem newItem (final String filePath) {
		return new MixedMediaItem(filePath, this.list);
	}

}
