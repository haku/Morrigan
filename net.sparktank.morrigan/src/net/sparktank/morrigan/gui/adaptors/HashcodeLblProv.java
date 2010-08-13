package net.sparktank.morrigan.gui.adaptors;

import net.sparktank.morrigan.model.media.impl.MediaItem;

import org.eclipse.jface.viewers.ColumnLabelProvider;

public class HashcodeLblProv extends ColumnLabelProvider {
	
	public HashcodeLblProv () {/* UNUSED */}
	
	@Override
	public String getText(Object element) {
		MediaItem elm = (MediaItem) element;
		if (elm.getHashcode() == 0) {
			return null;
		}
		
		return Long.toHexString(elm.getHashcode());
	}
	
}