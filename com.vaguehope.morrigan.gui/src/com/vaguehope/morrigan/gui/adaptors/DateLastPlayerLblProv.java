package com.vaguehope.morrigan.gui.adaptors;

import java.text.SimpleDateFormat;


import org.eclipse.jface.viewers.ColumnLabelProvider;

import com.vaguehope.morrigan.model.media.IMediaTrack;

public class DateLastPlayerLblProv extends ColumnLabelProvider {
	
	public DateLastPlayerLblProv () {/* UNUSED */}
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	@Override
	public String getText(Object element) {
		IMediaTrack elm = (IMediaTrack) element;
		return elm.getDateLastPlayed() == null ? null : this.sdf.format(elm.getDateLastPlayed());
	}
	
}
