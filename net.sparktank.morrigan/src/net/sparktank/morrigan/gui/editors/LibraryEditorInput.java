package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.model.MediaSqliteLayer2.DbColumn;
import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;
import net.sparktank.morrigan.model.tracks.library.AbstractMediaLibrary;

import org.eclipse.ui.IMemento;

public class LibraryEditorInput extends MediaTrackListEditorInput<AbstractMediaLibrary> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public LibraryEditorInput(AbstractMediaLibrary mediaList) {
		super(mediaList);
	}
	
	@Override
	public void saveState(IMemento memento) {
		DbColumn sort = getMediaList().getSort();
		memento.putString(EditorFactory.KEY_LIB_SORTCOL, sort.getName());
		
		SortDirection dir = getMediaList().getSortDirection();
		memento.putString(EditorFactory.KEY_LIB_SORTDIR, dir.name());
		
		super.saveState(memento);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
