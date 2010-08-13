package net.sparktank.morrigan.gui.helpers;

import java.util.HashMap;
import java.util.Map;

import net.sparktank.morrigan.gui.Activator;

import org.eclipse.swt.graphics.Image;

public class ImageCache {
	
	private Map<String, Image> imageCache = new HashMap<String, Image>();
	
	public void clearCache () {
		for (String i : this.imageCache.keySet()) {
			this.imageCache.get(i).dispose();
		}
	}
	
	public Image readImage (String path) {
		if (!this.imageCache.containsKey(path)) {
			this.imageCache.put(path, Activator.getImageDescriptor(path).createImage());
		}
		return this.imageCache.get(path);
	}
	
}
