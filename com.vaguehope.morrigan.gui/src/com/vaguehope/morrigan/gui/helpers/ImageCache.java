package com.vaguehope.morrigan.gui.helpers;

import java.util.HashMap;
import java.util.Map;


import org.eclipse.swt.graphics.Image;

import com.vaguehope.morrigan.gui.Activator;

/**
 * TODO replace this class with Activator.plugin.getImageRegistry()
 */
public class ImageCache {
	
	private Map<String, Image> imageCache = new HashMap<String, Image>();
	
	synchronized public void clearCache () {
		for (Image i : this.imageCache.values()) {
			i.dispose();
		}
		this.imageCache.clear();
	}
	
	public Image readImage (String path) {
		if (!this.imageCache.containsKey(path)) {
			this.imageCache.put(path, Activator.getImageDescriptor(path).createImage());
		}
		return this.imageCache.get(path);
	}
	
}
