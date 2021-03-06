package com.vaguehope.morrigan.gui.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.editors.EditorFactory;
import com.vaguehope.morrigan.gui.editors.MediaItemDbEditorInput;
import com.vaguehope.morrigan.gui.editors.mmdb.RemoteMixedMediaDbEditor;
import com.vaguehope.morrigan.gui.views.ViewMediaExplorer;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.server.MlistsServlet;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbHelper;


public class NewRemoteMixedDbAction extends Action implements IWorkbenchAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String DEFAULT_URL = "http://localhost:8080" + MlistsServlet.CONTEXTPATH + "/" + MediaListType.LOCALMMDB + "/mymmdb.local.db3";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final IWorkbenchWindow window;

	public NewRemoteMixedDbAction (final IWorkbenchWindow window) {
		super();
		this.window = window;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getText() { return "New remote mixed media db..."; }

	@Override
	public String getId() { return "newRMMDB"; }

	@Override
	public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
		// TODO choose icon.
		return Activator.getImageDescriptor("icons/db-remote.png");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void run () {
		InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), "New remote DB", "Enter MMDB URL.", DEFAULT_URL, null);
		if (dlg.open() != Window.OK) return;
		String url = dlg.getValue();

		dlg = new InputDialog(Display.getCurrent().getActiveShell(), "New remote DB", "Enter pass.", null, null);
		if (dlg.open() != Window.OK) return;
		String pass = dlg.getValue();

		IRemoteMixedMediaDb createdRemoteMmdb;
		try {
			createdRemoteMmdb = RemoteMixedMediaDbHelper.createRemoteMmdb(Config.DEFAULT, url, pass);
		}
		catch (Exception e) {
			new MorriganMsgDlg(e).open();
			return;
		}

		// refresh explorer.
		IWorkbenchPage page = this.window.getActivePage();
		ViewMediaExplorer view = (ViewMediaExplorer) page.findView(ViewMediaExplorer.ID);
		view.refresh();

		// Open new item.
		try {
			MediaItemDbEditorInput input = EditorFactory.getRemoteMmdbInput(createdRemoteMmdb.getDbPath());
			page.openEditor(input, RemoteMixedMediaDbEditor.ID);
		}
		catch (PartInitException e) {
			new MorriganMsgDlg(e).open();
		} catch (MorriganException e) {
			new MorriganMsgDlg(e).open();
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void dispose() {/* UNUSED */}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
