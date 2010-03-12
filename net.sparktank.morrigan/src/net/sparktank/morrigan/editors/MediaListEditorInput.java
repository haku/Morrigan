package net.sparktank.morrigan.editors;

import net.sparktank.morrigan.helpers.EqualHelper;
import net.sparktank.morrigan.model.media.MediaList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

public class MediaListEditorInput<T extends MediaList> implements IEditorInput, IPersistableElement {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final T editedMediaList;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MediaListEditorInput (T mediaList) {
		editedMediaList = mediaList;
	}
	
	public T getMediaList() {
		return editedMediaList;
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
