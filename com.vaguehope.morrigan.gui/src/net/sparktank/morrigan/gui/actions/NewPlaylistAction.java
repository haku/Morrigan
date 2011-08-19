package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.EditorFactory;
import net.sparktank.morrigan.gui.editors.MediaItemListEditorInput;
import net.sparktank.morrigan.gui.editors.tracks.PlaylistEditor;
import net.sparktank.morrigan.gui.views.ViewMediaExplorer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaPlaylist;
import com.vaguehope.morrigan.model.media.impl.MediaFactoryImpl;


public class NewPlaylistAction extends Action implements IWorkbenchAction {
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
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run () {
		InputDialog dlg = new InputDialog(
				Display.getCurrent().getActiveShell(),
				"", "Enter playlist name.", "newPl", null);
		if (dlg.open() == Window.OK) {
			
			// create playlist.
			String plName = dlg.getValue();
			IMediaPlaylist createdPl;
			try {
				createdPl = MediaFactoryImpl.get().createPlaylist(plName);
			} catch (MorriganException e) {
				new MorriganMsgDlg(e).open();
				return;
			}
			
			// refresh explorer.
			IWorkbenchPage page = this.window.getActivePage();
			ViewMediaExplorer view = (ViewMediaExplorer) page.findView(ViewMediaExplorer.ID);
			view.refresh();
			
			// Open new item.
			try {
				MediaItemListEditorInput<IMediaPlaylist> input = EditorFactory.getMediaPlaylistInput(createdPl.getFilePath());
				page.openEditor(input, PlaylistEditor.ID);
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
