package net.sparktank.morrigan.gui.editors.pictures;

import java.util.LinkedList;
import java.util.List;

import net.sparktank.morrigan.gui.adaptors.DateAddedLblProv;
import net.sparktank.morrigan.gui.adaptors.DateLastModifiedLblProv;
import net.sparktank.morrigan.gui.adaptors.FileLblProv;
import net.sparktank.morrigan.gui.adaptors.HashcodeLblProv;
import net.sparktank.morrigan.gui.editors.MediaColumn;
import net.sparktank.morrigan.gui.editors.MediaItemListEditor;
import net.sparktank.morrigan.model.pictures.IMediaPictureList;
import net.sparktank.morrigan.model.pictures.MediaPicture;

import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;

public abstract class MediaPictureListEditor<T extends IMediaPictureList<S>, S extends MediaPicture> extends MediaItemListEditor<T, S> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Column definitions.
	
	public final MediaColumn 
		COL_FILE =       new MediaColumn("file",        new ColumnWeightData(100),            new FileLblProv(this.getImageCache()) );
	public final MediaColumn 
		COL_ADDED =      new MediaColumn("added",       new ColumnPixelData(140, true, true), new DateAddedLblProv()           );
	public final MediaColumn 
		COL_HASH =       new MediaColumn("hash",        new ColumnPixelData( 90, true, true), new HashcodeLblProv(),           SWT.CENTER);
	public final MediaColumn 
		COL_MODIFIED =   new MediaColumn("modified",    new ColumnPixelData(140, true, true), new DateLastModifiedLblProv()    );
	
	public final MediaColumn[] COLS = new MediaColumn[] {
		this.COL_FILE,
		this.COL_ADDED,
		this.COL_HASH,
		this.COL_MODIFIED
	};
	
	@Override
	protected List<MediaColumn> getColumns() {
		List<MediaColumn> l = new LinkedList<MediaColumn>();
		for (MediaColumn c : this.COLS) {
			l.add(c);
		}
		return l;
	}
	
	@Override
	protected boolean isColumnVisible(MediaColumn col) {
		return true;
	}
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
