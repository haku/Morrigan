package net.sparktank.morrigan.gui.adaptors;

import java.text.SimpleDateFormat;

import net.sparktank.morrigan.model.media.interfaces.IMediaItem;

import org.eclipse.jface.viewers.ColumnLabelProvider;

public class DateLastModifiedLblProv extends ColumnLabelProvider {
	
	public DateLastModifiedLblProv () {/* UNUSED */}
	
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	@Override
	public String getText(Object element) {
		IMediaItem elm = (IMediaItem) element;
		return elm.getDateLastModified() == null ? null : this.sdf.format(elm.getDateLastModified());
	}
	
}
