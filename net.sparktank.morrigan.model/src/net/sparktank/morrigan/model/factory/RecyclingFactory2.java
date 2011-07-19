package net.sparktank.morrigan.model.factory;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * T = what to make (product).
 * K = what to make it from (material).
 * S = thrown by constructor.
 */
public abstract class RecyclingFactory2<T extends Object, K extends Object, S extends Throwable> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Map<K, WeakReference<T>> cache = new HashMap<K, WeakReference<T>>();
	private final boolean allowRecycle;
	
	protected RecyclingFactory2 (boolean allowReuse) {
		this.allowRecycle = allowReuse;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public synchronized T manufacture (K material) throws S {
		return manufacture(material, false);
	}
	
	public synchronized T manufacture (K material, boolean forceCompletlyNew) throws S {
		T ret = null;
		
		if (forceCompletlyNew) {
			ret = makeNewProduct(material);
		}
		else {
			// See if already have a matching product we made earlier.
			WeakReference<T> wr = this.cache.get(material);
			if (wr != null) {
				ret = wr.get();
				if (ret == null) {
					this.cache.remove(material);
				}
			}
			
			// If an object is found, check it is still valid.
			if (ret != null && !isValidProduct(ret)) {
				this.cache.remove(material);
				ret = null;
			}
			
			// If no reusable product found, make one.
			// If we found one, but are not allowed to use it, return null.
			if (ret == null) {
				ret = makeNewProduct(material);
				this.cache.put(material, new WeakReference<T>(ret));
			}
			else if (!this.allowRecycle) {
				ret = null;
			}
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public synchronized void disposeAll () {
		Set<Entry<K,WeakReference<T>>> products = new HashSet<Entry<K, WeakReference<T>>>(this.cache.entrySet());
		
		for (Entry<K, WeakReference<T>> entry : products) {
			T product = entry.getValue().get();
			if (product != null) {
				disposeProduct(product);
				this.cache.remove(entry.getKey());
			}
		}
	}
	
	protected void disposeProduct (T product) {
		throw new IllegalArgumentException("Not implemented.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * @throws S  
	 */
	protected T makeNewProduct (K material) throws S {
		throw new IllegalArgumentException("Not implemented.");
	}
	
	protected abstract boolean isValidProduct (T product);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
