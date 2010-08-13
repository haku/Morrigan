package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.model.MediaItemDb;
import net.sparktank.morrigan.model.MediaSqliteLayer2;
import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.morrigan.model.media.impl.MediaItem;

import org.eclipse.ui.IMemento;

/**
 * Subclass of MediaItemListEditorInput to allow saving of sort configuration.
 */
public class MediaItemDbEditorInput extends MediaItemListEditorInput<MediaItemDb<? extends MediaSqliteLayer2<? extends MediaItem>, ? extends MediaItem>> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaItemDbEditorInput(MediaItemDb<? extends MediaSqliteLayer2<? extends MediaItem>, ? extends MediaItem> mediaList) {
		super(mediaList);
	}
	
	@Override
	public void saveState(IMemento memento) {
		IDbColumn sort = getMediaList().getSort();
		memento.putString(EditorFactory.KEY_LIB_SORTCOL, sort.getName());
		
		SortDirection dir = getMediaList().getSortDirection();
		memento.putString(EditorFactory.KEY_LIB_SORTDIR, dir.name());
		
		super.saveState(memento);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
