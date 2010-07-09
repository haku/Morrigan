package net.sparktank.morrigan.helpers;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * T = what to make (product).
 * K = what to make it from (material).
 * P = Non-unique config to use when making.
 * S = thrown by constructor.
 */
public abstract class RecyclingFactory<T extends Object, K extends Object, P extends Object, S extends Throwable> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Map<K, WeakReference<T>> cache = new HashMap<K, WeakReference<T>>();
	private final boolean allowRecycle;
	
	protected RecyclingFactory (boolean allowReuse) {
		this.allowRecycle = allowReuse;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public synchronized T manufacture (K material) throws S {
		return manufacture(material, null);
	}
	
	public synchronized T manufacture (K material, P config) throws S {
		T ret = null;
		
		// See if already have a matching product we made earlier.
		WeakReference<T> wr = cache.get(material);
		if (wr != null) {
			ret = wr.get();
			if (ret == null) {
				cache.remove(material);
			}
		}
		
		// If an object is found, check it is still valid.
		if (ret != null && !isValidProduct(ret)) {
			cache.remove(material);
			ret = null;
		}
		
		// If no reusable product found, make one.
		// If we found one, but are not allowed to use it, return null.
		if (ret == null) {
			if (config == null) {
				ret = makeNewProduct(material);
			} else {
				ret = makeNewProduct(material, config);
			}
			cache.put(material, new WeakReference<T>(ret));
		}
		else if (!allowRecycle) {
			ret = null;
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public synchronized void disposeAll () {
		Set<Entry<K,WeakReference<T>>> products = new HashSet<Entry<K, WeakReference<T>>>(cache.entrySet());
		
		for (Entry<K, WeakReference<T>> entry : products) {
			T product = entry.getValue().get();
			if (product != null) {
				disposeProduct(product);
				cache.remove(entry.getKey());
			}
		}
	}
	
	protected void disposeProduct (T product) {
		throw new IllegalArgumentException("Not implemented.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected T makeNewProduct (K material) throws S {
		throw new IllegalArgumentException("Not implemented.");
	}
	
	protected T makeNewProduct (K material, P config) throws S {
		throw new IllegalArgumentException("Not implemented.");
	}
	
	protected abstract boolean isValidProduct (T product);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
