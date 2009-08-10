package net.sparktank.morrigan.editors;

import net.sparktank.morrigan.model.media.MediaList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class MediaListEditor extends EditorPart {
	public static final String ID = "net.sparktank.morrigan.editors.MediaListEditor";
	private MediaList editedMediaList;
	
	public MediaListEditor() {
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO save changes.
		
//		person.getAddress().setCountry(text2.getText());
	}
	
	@Override
	public void doSaveAs() {
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		editedMediaList = ((MediaListEditorInput) input).getEditedMediaList();
		setPartName(editedMediaList.getListName());
	}
	
	@Override
	public boolean isDirty() {
		
		// TODO
		
		return false;
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		
		// TODO display this as a list.
		
		Text text = new Text(parent, SWT.BORDER + SWT.MULTI + SWT.H_SCROLL + SWT.V_SCROLL);
		text.setText(editedMediaList.toString());
	}
	
	@Override
	public void setFocus() {
	}
	
}
