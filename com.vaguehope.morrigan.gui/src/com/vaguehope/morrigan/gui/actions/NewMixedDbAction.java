package com.vaguehope.morrigan.gui.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.editors.EditorFactory;
import com.vaguehope.morrigan.gui.editors.MediaItemDbEditorInput;
import com.vaguehope.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import com.vaguehope.morrigan.gui.views.ViewMediaExplorer;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;

public class NewMixedDbAction extends Action implements IWorkbenchAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private IWorkbenchWindow window;

	public NewMixedDbAction (IWorkbenchWindow window) {
		super();
		this.window = window;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getText() { return "New mixed db..."; }

	@Override
	public String getId() { return "newmixeddb"; }

	@Override
	public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
		// TODO choose icon.
		return Activator.getImageDescriptor("icons/db.png");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void run () {
		InputDialog dlg = new InputDialog(
				Display.getCurrent().getActiveShell(),
				"New DB", "Enter a file name for the new DB.", "newMMDB", null);
		if (dlg.open() == Window.OK) {
			// create library.
			String libName = dlg.getValue();
			ILocalMixedMediaDb createdMmdb;
			try {
				createdMmdb = Activator.getMediaFactory().createLocalMixedMediaDb(libName);
			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
				return;
			}

			// refresh explorer.
			IWorkbenchPage page = this.window.getActivePage();
			ViewMediaExplorer view = (ViewMediaExplorer) page.findView(ViewMediaExplorer.ID);
			view.refresh();

			// Open new item.
			try {
				MediaItemDbEditorInput input = EditorFactory.getMmdbInput(createdMmdb.getDbPath());
				page.openEditor(input, LocalMixedMediaDbEditor.ID);
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
	public void dispose() {/* UNUSED */}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
