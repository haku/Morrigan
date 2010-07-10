package net.sparktank.nemain.helpers;

import java.util.HashMap;
import java.util.Map;

import net.sparktank.nemain.Activator;

import org.eclipse.swt.graphics.Image;

public class ImageCache {
	
	private Map<String, Image> imageCache = new HashMap<String, Image>();
	
	public void clearCache () {
		for (String i : imageCache.keySet()) {
			imageCache.get(i).dispose();
		}
	}
	
	public Image readImage (String path) {
		if (!imageCache.containsKey(path)) {
			imageCache.put(path, Activator.getImageDescriptor(path).createImage());
		}
		return imageCache.get(path);
	}
	
}
