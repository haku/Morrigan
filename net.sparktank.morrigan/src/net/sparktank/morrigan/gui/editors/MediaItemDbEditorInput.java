package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.model.DbColumn;
import net.sparktank.morrigan.model.IMediaItemList;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaItemDb;
import net.sparktank.morrigan.model.MediaSqliteLayer2;
import net.sparktank.morrigan.model.MediaSqliteLayer2.SortDirection;

import org.eclipse.ui.IMemento;

/**
 * Subclass of MediaItemListEditorInput to allow saving of sort configuration.
 */
public class MediaItemDbEditorInput extends MediaItemListEditorInput<MediaItemDb<? extends IMediaItemList<? extends MediaItem>, ? extends MediaSqliteLayer2<? extends MediaItem>, ? extends MediaItem>> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaItemDbEditorInput(MediaItemDb<? extends IMediaItemList<? extends MediaItem>, ? extends MediaSqliteLayer2<? extends MediaItem>, ? extends MediaItem> mediaList) {
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
