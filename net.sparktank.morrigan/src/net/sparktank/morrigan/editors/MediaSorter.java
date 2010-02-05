package net.sparktank.morrigan.editors;

import net.sparktank.morrigan.model.media.MediaTrack;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class MediaSorter extends ViewerSorter {
	
	private int propertyIndex;
	 private static final int ASCENDING = 0;
	private static final int DESCENDING = 1;
	
	private int direction = ASCENDING;
	
	public MediaSorter () {
		this.propertyIndex = 0;
	}
	
	public void setColumn(int column) {
		if (column == this.propertyIndex) {
			direction = 1 - direction;
		} else {
			this.propertyIndex = column;
			direction = DESCENDING;
		}
	}
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		MediaTrack t1 = (MediaTrack) e1;
		MediaTrack t2 = (MediaTrack) e2;
		
		int rc = 0;
		
		switch (propertyIndex) {
			case 0:
				rc = t1.getFilepath().compareTo(t2.getFilepath());
				break;
				
			default:
				rc = 0;
		}
		
		if (direction == DESCENDING) rc = -rc;
		
		return rc;
	}
	
	private MediaSorter getThis () {
		return this;
	}
	
	public SelectionAdapter getSelectionAdapter (final TableViewer table, final int index, final TableViewerColumn column) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getThis().setColumn(index);
				int dir = table.getTable().getSortDirection();
				if (table.getTable().getSortColumn() == column.getColumn()) {
					dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
				} else {
					dir = SWT.DOWN;
				}
				table.getTable().setSortDirection(dir);
				table.getTable().setSortColumn(column.getColumn());
				table.refresh();
			}
		};
	}
}
