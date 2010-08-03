package net.sparktank.morrigan.gui.editors;

import java.util.Date;

import net.sparktank.morrigan.gui.editors.MediaTrackListEditor.MediaColumn;
import net.sparktank.morrigan.model.tracks.MediaTrack;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class MediaSorter extends ViewerSorter {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final int ASCENDING = 0;
	private static final int DESCENDING = 1;
	
	private MediaColumn column;
	private int direction;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaSorter () {
		setColumn(MediaColumn.FILE);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setColumn(MediaColumn column) {
		if (column == this.column) {
			direction = 1 - direction;
		} else {
			this.column = column;
			direction = ASCENDING;
		}
	}
	
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		MediaTrack t1 = (MediaTrack) e1;
		MediaTrack t2 = (MediaTrack) e2;
		
		int rc = 0;
		
		switch (column) {
			case FILE:
				rc = t1.getFilepath().compareTo(t2.getFilepath());
				break;
				
			case COUNTS:
				rc = compareLongs(t1.getStartCount(), t2.getStartCount());
				break;
				
			case DURATION:
				rc = t1.getDuration() - t2.getDuration();
				break;
				
			case DADDED:
				rc = compareDates(t1.getDateAdded(), t2.getDateAdded());
				break;
				
			case DLASTPLAY:
				rc = compareDates(t1.getDateLastPlayed(), t2.getDateLastPlayed());
				break;
				
			case HASHCODE:
				rc = compareLongs(t1.getHashcode(), t2.getHashcode());
				break;
				
			default:
				rc = 0;
		}
		
		if (direction == DESCENDING) rc = -rc;
		
		return rc;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaSorter getThis () {
		return this;
	}
	
	public SelectionAdapter getSelectionAdapter (final TableViewer table, 
			final MediaColumn mediaColumn, final TableViewerColumn tableColumn) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getThis().setColumn(mediaColumn);
				
				if (tableColumn != null) {
					table.getTable().setSortDirection(direction == ASCENDING ? SWT.UP : SWT.DOWN);
					table.getTable().setSortColumn(tableColumn.getColumn());
					
				} else {
					table.getTable().setSortDirection(SWT.NONE);
					table.getTable().setSortColumn(null);
				}
				
				table.refresh();
			}
		};
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private int compareLongs (long l1, long l2) {
		if (l1 < l2) {
			return -1;
		} else if (l1 > l2) {
			return +1;
		}
		return 0;
	}
	
	private int compareDates (Date d1, Date d2) {
		long t1 = 0;
		long t2 = 0;
		
		if (d1 != null) t1 = d1.getTime();
		if (d2 != null) t2 = d2.getTime();
		
		return compareLongs(t1, t2);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
