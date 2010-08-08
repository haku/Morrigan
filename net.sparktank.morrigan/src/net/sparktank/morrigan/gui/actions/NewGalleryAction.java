package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.EditorFactory;
import net.sparktank.morrigan.gui.editors.LibraryEditorInput;
import net.sparktank.morrigan.gui.views.ViewMediaExplorer;
import net.sparktank.morrigan.model.pictures.gallery.LocalGallery;
import net.sparktank.morrigan.model.pictures.gallery.LocalGalleryHelper;
import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

public class NewGalleryAction extends Action implements IWorkbenchAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IWorkbenchWindow window;
	
	public NewGalleryAction (IWorkbenchWindow window) {
		super();
		this.window = window;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { return "New gallery..."; }
	
	@Override
	public String getId() { return "newgallery"; }
	
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
				"", "Enter gallery name.", "newGallery", null);
		if (dlg.open() == Window.OK) {
			
			// create library.
			String libName = dlg.getValue();
			LocalGallery createdGallery;
			try {
				createdGallery = LocalGalleryHelper.createGallery(libName);
			} catch (DbException e) {
				new MorriganMsgDlg(e).open();
				return;
			}
			
			// refresh explorer.
			IWorkbenchPage page = this.window.getActivePage();
			ViewMediaExplorer view = (ViewMediaExplorer) page.findView(ViewMediaExplorer.ID);
			view.refresh();
			
			// Open new item.
//			try {
				LibraryEditorInput input = EditorFactory.getGalleryInput(createdGallery.getDbPath());
				throw new RuntimeException("Not implemented.");
//				page.openEditor(input, GalleryEditor.ID); // TODO
//			}
//			catch (PartInitException e) {
//				new MorriganMsgDlg(e).open();
//			} catch (MorriganException e) {
//				new MorriganMsgDlg(e).open();
//			}
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void dispose() {/* UNUSED */}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
