package net.sparktank.morrigan.gui.adaptors;

import net.sparktank.morrigan.model.tracks.MediaTrack;

import org.eclipse.jface.viewers.ColumnLabelProvider;

public class CountsLblProv extends ColumnLabelProvider {
	
	public CountsLblProv () {/* UNUSED */}
	
	@Override
	public String getText(Object element) {
		MediaTrack elm = (MediaTrack) element;
		if (elm.getStartCount() <= 0 && elm.getStartCount() <= 0) {
			return null;
		}
		
		return String.valueOf(elm.getStartCount()) + "/" + String.valueOf(elm.getEndCount());
	}
	
}