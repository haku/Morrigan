package net.sparktank.morrigan.handler;

import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.editors.EditorFactory;
import net.sparktank.morrigan.editors.LibraryEditor;
import net.sparktank.morrigan.editors.MediaListEditorInput;
import net.sparktank.morrigan.editors.PlaylistEditor;
import net.sparktank.morrigan.model.media.MediaLibrary;
import net.sparktank.morrigan.model.media.MediaPlaylist;
import net.sparktank.morrigan.model.ui.MediaExplorerItem;
import net.sparktank.morrigan.views.ViewMediaExplorer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class CallMediaListEditor extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.handler.CallMediaListEditor";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// FIXME work out how to pass paramaters correctly.
		
		// Get the view
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		ViewMediaExplorer view = (ViewMediaExplorer) page.findView(ViewMediaExplorer.ID);
		
		// Get the selection
		ISelection selection = view.getSite().getSelectionProvider().getSelection();
		if (selection != null && selection instanceof IStructuredSelection) {
			// since we are not dealing with multi-select yet...
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			
			// If we had a selection lets open the editor.
			if (obj != null) {
				MediaExplorerItem item = (MediaExplorerItem) obj;
				
				if (item.type == MediaExplorerItem.ItemType.PLAYLIST) {
					try {
						MediaListEditorInput<MediaPlaylist> input = EditorFactory.getMediaPlaylistInput(item.identifier);
						page.openEditor(input, PlaylistEditor.ID);
					} catch (Exception e) {
						new MorriganMsgDlg(e).open();
						return null;
					}
					
				} else if (item.type == MediaExplorerItem.ItemType.LIBRARY) {
					try {
						MediaListEditorInput<MediaLibrary> input = EditorFactory.getMediaLibraryInput(item.identifier);
						page.openEditor(input, LibraryEditor.ID);
					} catch (Exception e) {
						new MorriganMsgDlg(e).open();
						return null;
					}
					
				} else {
					new MorriganMsgDlg("TODO: show " + item.identifier).open();
				}
			}
		}
		return null;
	}

}
