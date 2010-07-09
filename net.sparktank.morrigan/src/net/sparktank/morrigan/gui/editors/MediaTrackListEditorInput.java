package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.helpers.EqualHelper;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaItemList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

public class MediaTrackListEditorInput<T extends MediaItemList<? extends MediaItem>> implements IEditorInput, IPersistableElement {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final T editedMediaList;
	private int topIndex = -1;
	private Table table;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.
	
	public MediaTrackListEditorInput (T mediaList) {
		editedMediaList = mediaList;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public T getMediaList() {
		return editedMediaList;
	}
	
	public void setTable (Table table) {
		this.table = table;
	}
	
	public int getTopIndex () {
		return topIndex;
	}
	
	public void setTopIndex (int i) {
		this.topIndex = i;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getName() {
		return editedMediaList.toString();
	}
	
	@Override
	public String getToolTipText() {
		return editedMediaList.toString();
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public IPersistableElement getPersistable() {
		return this;
	}
	
	@Override
	public boolean exists() {
		return true;
	}
	
	@Override
	public String getFactoryId() {
		return EditorFactory.ID;
	}
	
	@Override
	public void saveState(IMemento memento) {
		memento.putString(EditorFactory.KEY_TYPE, editedMediaList.getType());
		memento.putString(EditorFactory.KEY_SERIAL, editedMediaList.getSerial());
		if (table != null && !table.isDisposed()) {
			memento.putString(EditorFactory.KEY_TOPINDEX, String.valueOf(table.getTopIndex()));
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public boolean equals(Object aThat) {
		if ( aThat == null ) return false;
		if ( this == aThat ) return true;
		if ( !(aThat instanceof MediaTrackListEditorInput<?>) ) return false;
		MediaTrackListEditorInput<?> that = (MediaTrackListEditorInput<?>)aThat;
		
		return EqualHelper.areEqual(editedMediaList.getListId(), that.getMediaList().getListId());
	}
	
	@Override
	public int hashCode() {
		return editedMediaList.getListId().hashCode();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
