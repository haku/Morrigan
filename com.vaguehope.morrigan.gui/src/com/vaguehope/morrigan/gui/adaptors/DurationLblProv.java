package com.vaguehope.morrigan.gui.adaptors;


import org.eclipse.jface.viewers.ColumnLabelProvider;

import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.util.TimeHelper;

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
