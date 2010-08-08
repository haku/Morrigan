package net.sparktank.morrigan.gui.adaptors;

import net.sparktank.morrigan.model.MediaItem;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class MediaFilter<T, S> extends ViewerFilter {
	
	private String searchTerm;

	public void setFilterString (String s) {
		this.searchTerm = ".*(?i)" + s + ".*";
		
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (this.searchTerm == null || this.searchTerm.length() == 0) {
			return true;
		}
		MediaItem mi = (MediaItem) element;
		if (mi.getFilepath().matches(this.searchTerm)) {
			return true;
		}
		return false;
	}
	
}