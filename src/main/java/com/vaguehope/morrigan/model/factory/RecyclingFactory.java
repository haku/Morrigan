package com.vaguehope.morrigan.model.factory;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * T = what to make (product).
 * K = what to make it from (material).
 * P = Non-unique config to use when making.
 * S = thrown by constructor.
 */
public abstract class RecyclingFactory<T extends Object, K extends Object, P extends Object, S extends Throwable> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private Map<K, WeakReference<T>> cache = new ConcurrentHashMap<K, WeakReference<T>>();
	private final boolean allowRecycle;

	protected RecyclingFactory (boolean allowReuse) {
		this.allowRecycle = allowReuse;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public synchronized T manufacture (K material) throws S {
		return manufacture(material, null, false);
	}

	public synchronized T manufacture (K material, boolean forceCompletlyNew) throws S {
		return manufacture(material, null, forceCompletlyNew);
	}

	public synchronized T manufacture (K material, P config) throws S {
		return manufacture(material, config, false);
	}

	public synchronized T manufacture (K material, P config, boolean forceCompletlyNew) throws S {
		T ret = null;

		if (forceCompletlyNew) {
			if (config == null) {
				ret = makeNewProduct(material);
			} else {
				ret = makeNewProduct(material, config);
			}
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
				if (config == null) {
					ret = makeNewProduct(material);
				} else {
					ret = makeNewProduct(material, config);
				}
				this.cache.put(material, new WeakReference<T>(ret));
			}
			else if (!this.allowRecycle) {
				ret = null;
			}
		}

		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * @throws S
	 */
	protected T makeNewProduct (K material) throws S {
		throw new IllegalArgumentException("Not implemented.");
	}

	/**
	 * @throws S
	 */
	protected T makeNewProduct (K material, P config) throws S {
		throw new IllegalArgumentException("Not implemented.");
	}

	protected abstract boolean isValidProduct (T product);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
