package net.sparktank.morrigan.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.part.EditorPart;

public class SaveEditorAction extends Action implements IWorkbenchAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private EditorPart editor;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public SaveEditorAction (EditorPart editor) {
		super();
		this.editor = editor;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
//	@Override
//	public String getText() { return "Save"; }
//	
//	@Override
//	public String getId() { return "save"; }
//	
//	@Override
//	public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
//		return Activator.getImageDescriptor("icons/save.gif");
//	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run () {
		if (editor.getSite().getWorkbenchWindow() == null) {
			return;
		}
		editor.getSite().getPage().saveEditor(editor, false);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void dispose() {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
