package com.vaguehope.morrigan.gui.adaptors;

import java.math.BigInteger;


import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.vaguehope.morrigan.gui.helpers.ImageCache;
import com.vaguehope.morrigan.player.PlayItem;

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
			return item.toString();
		}
		return null;
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof PlayItem) {
			PlayItem item = (PlayItem) element;
			
			if (!item.hasList()) {
				return null;
			}
			else if (!item.hasTrack()) {
				return this.imageCache.readImage("icons/db.png");
			}
			else if (!item.getTrack().isEnabled()) {
				return this.imageCache.readImage("icons/noentry-red.png");
			}
			else if (item.getTrack().getHashcode() == null || item.getTrack().getHashcode().equals(BigInteger.ZERO)) {
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
