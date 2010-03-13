package net.sparktank.morrigan.editors;

import org.eclipse.ui.IMemento;

import net.sparktank.morrigan.library.SqliteLayer.LibrarySort;
import net.sparktank.morrigan.library.SqliteLayer.LibrarySortDirection;
import net.sparktank.morrigan.model.media.*;

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
