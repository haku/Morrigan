package net.sparktank.morrigan.gui.editors;

import org.eclipse.ui.IMemento;

import net.sparktank.morrigan.model.library.MediaLibrary;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySort;
import net.sparktank.morrigan.model.library.SqliteLayer.LibrarySortDirection;

public class LibraryEditorInput extends MediaListEditorInput<MediaLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public LibraryEditorInput(MediaLibrary mediaList) {
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
