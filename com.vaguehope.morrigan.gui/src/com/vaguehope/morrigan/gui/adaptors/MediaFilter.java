package com.vaguehope.morrigan.gui.adaptors;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class MediaFilter extends ViewerFilter {
	
	private String searchTerm;

	public void setFilterString (String s) {
		this.searchTerm = ".*(?i)" + s + ".*";
		
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (this.searchTerm == null || this.searchTerm.length() == 0) {
			return true;
		}
		if (element.toString().matches(this.searchTerm)) {
			return true;
		}
		return false;
	}
	
}
