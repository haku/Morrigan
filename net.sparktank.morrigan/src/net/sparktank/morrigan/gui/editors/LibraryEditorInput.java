package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.model.tracks.library.AbstractMediaLibrary;
import net.sparktank.morrigan.model.tracks.library.LibrarySqliteLayer.LibrarySort;
import net.sparktank.morrigan.model.tracks.library.LibrarySqliteLayer.LibrarySortDirection;

import org.eclipse.ui.IMemento;

public class LibraryEditorInput extends MediaTrackListEditorInput<AbstractMediaLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public LibraryEditorInput(AbstractMediaLibrary mediaList) {
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
