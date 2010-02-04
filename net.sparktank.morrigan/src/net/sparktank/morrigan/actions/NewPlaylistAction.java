package net.sparktank.morrigan.actions;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.PlaylistHelper;
import net.sparktank.morrigan.views.ViewMediaExplorer;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;


public class NewPlaylistAction extends MorriganAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IWorkbenchWindow window;
	
	public NewPlaylistAction (IWorkbenchWindow window) {
		super();
		this.window = window;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { return "New playlist..."; }
	
	@Override
	public String getId() { return "newpl"; }
	
	@Override
	public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
		// TODO choose icon.
		return Activator.getImageDescriptor("icons/playlist.gif");
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void runWithEvent(Event event) {
		InputDialog dlg = new InputDialog(
				Display.getCurrent().getActiveShell(),
				"", "Enter playlist name.", "newPl", null);
		if (dlg.open() == Window.OK) {
			
			// create playlist.
			String plName = dlg.getValue();
			try {
				PlaylistHelper.instance.createPl(plName);
			} catch (MorriganException e) {
				new MorriganMsgDlg(e).open();
				return;
			}
			
			// refresh explorer.
			IWorkbenchPage page = window.getActivePage();
			ViewMediaExplorer view = (ViewMediaExplorer) page.findView(ViewMediaExplorer.ID);
			view.refresh();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
