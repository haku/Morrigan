package net.sparktank.morrigan.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import net.sparktank.morrigan.model.MediaList;

public class MediaListEditorInput implements IEditorInput {

	private final MediaList editedMediaList;

	public MediaListEditorInput (MediaList mediaList) {
		editedMediaList = mediaList;
	}
	
	public MediaList getEditedMediaList() {
		return editedMediaList;
	}
	
	@Override
	public boolean exists() {
		return false;
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}
	
	@Override
	public String getName() {
		return editedMediaList.toString();
	}
	
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}
	
	@Override
	public String getToolTipText() {
		return editedMediaList.toString();
	}
	
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			return true;
		}
		if (obj instanceof MediaListEditorInput) {
			return editedMediaList.equals(((MediaListEditorInput) obj).getEditedMediaList());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return editedMediaList.hashCode();
	}
}
