package net.sparktank.morrigan.providers;

import net.sparktank.morrigan.model.MenuItem;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ViewMainContentProvider implements IStructuredContentProvider {
	
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}
	
	public void dispose() {
	}
	
	public Object[] getElements(Object parent) {
//		return new String[] { "Display", "Library", "PlayList01", "PlayList02", "PlayList03" };
		
		return new MenuItem[] {
				new MenuItem("dis", "display"),
				new MenuItem("lib", "library"),
				new MenuItem("pl1", "play list 1"),
				new MenuItem("pl2", "play list 2"),
				new MenuItem("pl3", "play list 3")
				};
		
	}
	
}