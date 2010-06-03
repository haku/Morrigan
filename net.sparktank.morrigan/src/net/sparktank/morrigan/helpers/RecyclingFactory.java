package net.sparktank.morrigan.helpers;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/* T = what to make (product).
 * K = what to make it from (material).
 */
public abstract class RecyclingFactory<T extends RecycliableProduct<K>, K extends Object> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Map<K, WeakReference<T>> cache = new HashMap<K, WeakReference<T>>();
	private final boolean allowRecycle;
	
	protected RecyclingFactory (boolean allowReuse) {
		this.allowRecycle = allowReuse;
	}
	
	public synchronized T manufacture (K material) {
		T ret = null;
		
		// See if already have a matching product we made earlier.
		WeakReference<T> wr = cache.get(material);
		if (wr != null) {
			ret = wr.get();
		}
		
		// If an object is found, check it is still valid.
		if (ret != null && !isValidProduct(ret)) {
			cache.remove(material);
			ret = null;
		}
		
		// If no reusable product found, make one.
		// If we found one, but are not allowed to use it, return null.
		if (ret == null) {
			ret = makeNewProduct(material);
			cache.put(material, new WeakReference<T>(ret));
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
