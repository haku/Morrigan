package net.sparktank.morrigan.gui.editors;

import net.sparktank.morrigan.helpers.EqualHelper;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;
import net.sparktank.morrigan.model.media.interfaces.IMediaItemList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

public class MediaItemListEditorInput<T extends IMediaItemList<? extends IMediaItem>> implements IEditorInput, IPersistableElement {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final T editedMediaList;
	private int topIndex = -1;
	private Table table;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.
	
	public MediaItemListEditorInput (T mediaList) {
		this.editedMediaList = mediaList;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public T getMediaList() {
		return this.editedMediaList;
	}
	
	public void setTable (Table table) {
		this.table = table;
	}
	
	public int getTopIndex () {
		return this.topIndex;
	}
	
	public void setTopIndex (int i) {
		this.topIndex = i;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getName() {
		return this.editedMediaList.toString();
	}
	
	@Override
	public String getToolTipText() {
		return this.editedMediaList.toString();
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
		memento.putString(EditorFactory.KEY_TYPE, this.editedMediaList.getType());
		memento.putString(EditorFactory.KEY_SERIAL, this.editedMediaList.getSerial());
		if (this.table != null && !this.table.isDisposed()) {
			memento.putString(EditorFactory.KEY_TOPINDEX, String.valueOf(this.table.getTopIndex()));
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
		if ( !(aThat instanceof MediaItemListEditorInput<?>) ) return false;
		MediaItemListEditorInput<?> that = (MediaItemListEditorInput<?>)aThat;
		
		return EqualHelper.areEqual(this.editedMediaList.getListId(), that.getMediaList().getListId());
	}
	
	@Override
	public int hashCode() {
		return this.editedMediaList.getListId().hashCode();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
