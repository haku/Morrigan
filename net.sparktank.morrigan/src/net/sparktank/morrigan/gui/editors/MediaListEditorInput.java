package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.helpers.EqualHelper;
import net.sparktank.morrigan.model.MediaList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

public class MediaListEditorInput<T extends MediaList> implements IEditorInput, IPersistableElement {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final T editedMediaList;
	private int topIndex = -1;
	private Table table;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.
	
	public MediaListEditorInput (T mediaList) {
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
	
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public boolean equals(Object aThat) {
		if ( aThat == null ) return false;
		if ( this == aThat ) return true;
		if ( !(aThat instanceof MediaListEditorInput<?>) ) return false;
		MediaListEditorInput<?> that = (MediaListEditorInput<?>)aThat;
		
		return EqualHelper.areEqual(editedMediaList.getListId(), that.getMediaList().getListId());
	}
	
	@Override
	public int hashCode() {
		return editedMediaList.getListId().hashCode();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
