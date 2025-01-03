package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import com.vaguehope.morrigan.model.factory.RecyclingFactory2;
import com.vaguehope.morrigan.model.media.IMediaItem;

/**
 * This object will be responsible for all caching of item instances
 * so that we don't have to have crazy stuff in other parts of the
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
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

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

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MixedMediaItemFactory () {
//		System.err.println("new MixedMediaItemFactory()");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public IMediaItem getNewMediaItem (final String filePath) {
		if (filePath == null) return newItem(null); // We can not cache these. :(
		return this.factory.manufacture(filePath);
	}

	static MixedMediaItem newItem (final String filePath) {
//		System.err.println("new MixedMediaItem("+filePath+")");
		MixedMediaItem item = new MixedMediaItem(filePath);
		return item;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
