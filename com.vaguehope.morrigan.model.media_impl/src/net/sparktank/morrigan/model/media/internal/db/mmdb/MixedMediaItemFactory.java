package net.sparktank.morrigan.model.media.internal.db.mmdb;

import com.vaguehope.morrigan.model.factory.RecyclingFactory2;

import net.sparktank.morrigan.model.media.IMixedMediaItem;

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
	
	private RecyclingFactory2<MixedMediaItem, String, RuntimeException> factory = new RecyclingFactory2<MixedMediaItem, String, RuntimeException> (true) {
		
		@Override
		protected boolean isValidProduct(MixedMediaItem product) {
			return true;
		}
		
		@Override
		protected MixedMediaItem makeNewProduct(String material) throws RuntimeException {
			return newItem(material);
		}
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected MixedMediaItemFactory () {
//		System.err.println("new MixedMediaItemFactory()");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public IMixedMediaItem getNewMediaItem (String filePath) {
		if (filePath == null) return newItem(filePath); // We can not cache these. :(
		return this.factory.manufacture(filePath);
	}
	
	static MixedMediaItem newItem (String filePath) {
//		System.err.println("new MixedMediaItem("+filePath+")");
		MixedMediaItem item = new MixedMediaItem(filePath);
		return item;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
