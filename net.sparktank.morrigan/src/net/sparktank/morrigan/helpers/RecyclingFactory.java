package net.sparktank.morrigan.helpers;

import java.util.WeakHashMap;

/* T = what to make (product).
 * K = what to make it from (material).
 */
public abstract class RecyclingFactory<T extends RecycliableProduct<K>, K extends Object> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private WeakHashMap<T, K> cache = new WeakHashMap<T, K>(); // FIXME There may be a better way to do this.
	private final boolean allowRecycle;
	
	protected RecyclingFactory (boolean allowReuse) {
		this.allowRecycle = allowReuse;
	}
	
	public synchronized T manufacture (K material) {
		T ret = null;
		
		// See if already have a matching product we made earlier.
		if (cache.containsValue(material)) {
			for (T existingProduct : cache.keySet()) {
				if (existingProduct.isMadeWithThisMaterial(material)) {
					ret = existingProduct;
				}
			}
		}
		
		// If its finished it does not count.
		if (ret != null && !isValidProduct(ret)) {
			cache.remove(ret);
			ret = null;
		}
		
		// If no reusable product found, make one.
		if (ret == null) {
			ret = makeNewProduct(material);
			cache.put(ret, material);
		}
		else if (!allowRecycle) {
			ret = null;
		}
		
		return ret;
	}
	
	protected abstract T makeNewProduct (K material);
	
	protected abstract boolean isValidProduct (T product);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
