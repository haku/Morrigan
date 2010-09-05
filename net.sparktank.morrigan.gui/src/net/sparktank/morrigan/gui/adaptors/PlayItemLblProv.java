package net.sparktank.morrigan.gui.adaptors;

import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.player.PlayItem;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

public class PlayItemLblProv implements ILabelProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final ImageCache imageCache;
	
	public PlayItemLblProv (ImageCache imageCache) {
		this.imageCache = imageCache;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ILabelProvider
	
	@Override
	public String getText(Object element) {
		if (element instanceof PlayItem) {
			PlayItem item = (PlayItem) element;
			return item.item.toString();
		}
		return null;
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof PlayItem) {
			PlayItem item = (PlayItem) element;
			
			if (item.list == null) {
				return null;
			}
			else if (item.item == null) {
				return this.imageCache.readImage("icons/db.png");
			}
			else if (!item.item.isEnabled()) {
				return this.imageCache.readImage("icons/noentry-red.png");
			}
			else if (item.item.getHashcode() == 0) {
				return this.imageCache.readImage("icons/exclamation-red.png");
			}
			else {
				return this.imageCache.readImage("icons/circledot.png");
			}
		}
		return null;
	}
	
	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}
	@Override
	public void removeListener(ILabelProviderListener listener) {/* UNUSED */}
	@Override
	public void dispose() {/* UNUSED */}
	@Override
	public void addListener(ILabelProviderListener listener) {/* UNUSED */}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
