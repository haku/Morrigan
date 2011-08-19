package net.sparktank.morrigan.gui.adaptors;


import org.eclipse.jface.viewers.ColumnLabelProvider;

import com.vaguehope.morrigan.model.media.IMediaTrack;

public class CountsLblProv extends ColumnLabelProvider {
	
	public CountsLblProv () {/* UNUSED */}
	
	@Override
	public String getText(Object element) {
		IMediaTrack elm = (IMediaTrack) element;
		if (elm.getStartCount() <= 0 && elm.getEndCount() <= 0) {
			return null;
		}
		
		return String.valueOf(elm.getStartCount()) + "/" + String.valueOf(elm.getEndCount());
	}
	
}