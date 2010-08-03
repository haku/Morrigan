package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.EditorFactory;
import net.sparktank.morrigan.gui.editors.LibraryEditorInput;
import net.sparktank.morrigan.gui.editors.LocalLibraryEditor;
import net.sparktank.morrigan.gui.views.ViewMediaExplorer;
import net.sparktank.morrigan.model.tracks.library.local.LocalLibraryHelper;
import net.sparktank.morrigan.model.tracks.library.local.LocalMediaLibrary;
import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

public class NewLibraryAction extends Action implements IWorkbenchAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IWorkbenchWindow window;
	
	public NewLibraryAction (IWorkbenchWindow window) {
		super();
		this.window = window;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { return "New library..."; }
	
	@Override
	public String getId() { return "newlib"; }
	
	@Override
	public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
		// TODO choose icon.
		return Activator.getImageDescriptor("icons/library.gif");
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run () {
		InputDialog dlg = new InputDialog(
				Display.getCurrent().getActiveShell(),
				"", "Enter library name.", "newLib", null);
		if (dlg.open() == Window.OK) {
			
			// create library.
			String libName = dlg.getValue();
			LocalMediaLibrary createdLib;
			try {
				createdLib = LocalLibraryHelper.createLib(libName);
			} catch (DbException e) {
				new MorriganMsgDlg(e).open();
				return;
			}
			
			// refresh explorer.
			IWorkbenchPage page = window.getActivePage();
			ViewMediaExplorer view = (ViewMediaExplorer) page.findView(ViewMediaExplorer.ID);
			view.refresh();
			
			// Open new item.
			try {
				LibraryEditorInput input = EditorFactory.getMediaLibraryInput(createdLib.getDbPath());
				page.openEditor(input, LocalLibraryEditor.ID);
			}
			catch (PartInitException e) {
				new MorriganMsgDlg(e).open();
			} catch (MorriganException e) {
				new MorriganMsgDlg(e).open();
			}
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void dispose() {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
