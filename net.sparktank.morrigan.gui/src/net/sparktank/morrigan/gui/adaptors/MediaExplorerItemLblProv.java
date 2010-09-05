package net.sparktank.morrigan.gui.adaptors;

import net.sparktank.morrigan.gui.helpers.ImageCache;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

public class MediaExplorerItemLblProv implements ILabelProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final ImageCache imageCache;
	
	public MediaExplorerItemLblProv (ImageCache imageCache) {
		this.imageCache = imageCache;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText(Object element) {
		if (element instanceof MediaExplorerItem) {
			MediaExplorerItem item = (MediaExplorerItem) element;
			return item.toString();
		}
		return null;
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof MediaExplorerItem) {
			MediaExplorerItem item = (MediaExplorerItem) element;
			switch (item.type) {
				
				case DISPLAY:
					return this.imageCache.readImage("icons/display.gif");
				
				case LOCALMMDB:
					return this.imageCache.readImage("icons/db.png");
				
				case REMOTEMMDB:
					return this.imageCache.readImage("icons/db-remote.png");
				
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
