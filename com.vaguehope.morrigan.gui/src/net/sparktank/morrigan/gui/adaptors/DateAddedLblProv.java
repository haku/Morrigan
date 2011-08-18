package net.sparktank.morrigan.gui.adaptors;

import java.text.SimpleDateFormat;

import net.sparktank.morrigan.model.media.IMediaItem;

import org.eclipse.jface.viewers.ColumnLabelProvider;

public class DateAddedLblProv extends ColumnLabelProvider {
	
	public DateAddedLblProv () {/* UNUSED */}
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	@Override
	public String getText(Object element) {
		IMediaItem elm = (IMediaItem) element;
		return elm.getDateAdded() == null ? null : this.sdf.format(elm.getDateAdded());
	}
	
}
