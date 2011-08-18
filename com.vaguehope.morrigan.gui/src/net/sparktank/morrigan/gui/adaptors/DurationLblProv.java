package net.sparktank.morrigan.gui.adaptors;

import net.sparktank.morrigan.model.media.IMediaTrack;
import net.sparktank.morrigan.util.TimeHelper;

import org.eclipse.jface.viewers.ColumnLabelProvider;

public class DurationLblProv extends ColumnLabelProvider {
	
	public DurationLblProv () {/* UNUSED */}
	
	@Override
	public String getText(Object element) {
		IMediaTrack elm = (IMediaTrack) element;
		if (elm.getDuration() <= 0) {
			return null;
		}
		
		return TimeHelper.formatTimeSeconds(elm.getDuration());
	}
	
}
