package net.sparktank.morrigan.gui.adaptors;


import org.eclipse.jface.viewers.ColumnLabelProvider;

import com.vaguehope.morrigan.model.media.IMediaPicture;

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