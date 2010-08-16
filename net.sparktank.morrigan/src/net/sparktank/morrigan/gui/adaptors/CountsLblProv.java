package net.sparktank.morrigan.gui.adaptors;

import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;

import org.eclipse.jface.viewers.ColumnLabelProvider;

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