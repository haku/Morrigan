package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.model.library.AbstractLibrary;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySort;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySortDirection;

import org.eclipse.ui.IMemento;

public class LibraryEditorInput extends MediaListEditorInput<AbstractLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public LibraryEditorInput(AbstractLibrary mediaList) {
		super(mediaList);
	}
	
	@Override
	public void saveState(IMemento memento) {
		LibrarySort sort = getMediaList().getSort();
		memento.putString(EditorFactory.KEY_LIB_SORTCOL, sort.name());
		
		LibrarySortDirection dir = getMediaList().getSortDirection();
		memento.putString(EditorFactory.KEY_LIB_SORTDIR, dir.name());
		
		super.saveState(memento);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
