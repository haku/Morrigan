package net.sparktank.morrigan.gui.adaptors;

import net.sparktank.morrigan.model.media.IMediaPicture;

import org.eclipse.jface.viewers.ColumnLabelProvider;

public class DimensionsLblProv extends ColumnLabelProvider {
	
	public DimensionsLblProv () {/* UNUSED */}
	
	@Override
	public String getText(Object element) {
		IMediaPicture i = (IMediaPicture) element;
		if (i.getWidth() <= 0 && i.getHeight() <= 0) {
			return null;
		}
		
		return String.valueOf(i.getWidth()) + "x" + String.valueOf(i.getHeight());
	}
	
}