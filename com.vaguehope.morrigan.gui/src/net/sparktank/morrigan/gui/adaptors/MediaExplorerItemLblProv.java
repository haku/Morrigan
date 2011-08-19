package net.sparktank.morrigan.gui.adaptors;

import net.sparktank.morrigan.gui.helpers.ImageCache;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.vaguehope.morrigan.model.media.MediaListReference;

public class MediaExplorerItemLblProv implements ILabelProvider {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final ImageCache imageCache;
	
	public MediaExplorerItemLblProv (ImageCache imageCache) {
		this.imageCache = imageCache;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText(Object element) {
		if (element instanceof MediaListReference) {
			MediaListReference item = (MediaListReference) element;
			return item.toString();
		}
		return null;
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public Image getImage(Object element) {
		if (element instanceof MediaListReference) {
			MediaListReference item = (MediaListReference) element;
			switch (item.getType()) {
				
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
