package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.model.MediaSqliteLayer2;
import net.sparktank.morrigan.model.db.interfaces.IDbColumn;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemDb;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemStorageLayer.SortDirection;

import org.eclipse.ui.IMemento;

/**
 * Subclass of MediaItemListEditorInput to allow saving of sort configuration.
 */
public class MediaItemDbEditorInput extends MediaItemListEditorInput<IMediaItemDb<? extends MediaSqliteLayer2<? extends IMediaItem>, ? extends IMediaItem>> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaItemDbEditorInput(IMediaItemDb<? extends MediaSqliteLayer2<? extends IMediaItem>, ? extends IMediaItem> mediaList) {
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
