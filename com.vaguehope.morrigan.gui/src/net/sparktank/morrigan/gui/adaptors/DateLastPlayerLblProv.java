package net.sparktank.morrigan.gui.adaptors;

import java.text.SimpleDateFormat;

import net.sparktank.morrigan.model.media.IMediaTrack;

import org.eclipse.jface.viewers.ColumnLabelProvider;

public class DateLastPlayerLblProv extends ColumnLabelProvider {
	
	public DateLastPlayerLblProv () {/* UNUSED */}
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	@Override
	public String getText(Object element) {
		IMediaTrack elm = (IMediaTrack) element;
		return elm.getDateLastPlayed() == null ? null : this.sdf.format(elm.getDateLastPlayed());
	}
	
}
