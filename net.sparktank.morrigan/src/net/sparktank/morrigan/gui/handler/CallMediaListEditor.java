package net.sparktank.morrigan.gui.handler;

import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.EditorFactory;
import net.sparktank.morrigan.gui.editors.MediaItemListEditorInput;
import net.sparktank.morrigan.gui.editors.pictures.LocalGalleryEditor;
import net.sparktank.morrigan.gui.editors.tracks.LocalLibraryEditor;
import net.sparktank.morrigan.gui.editors.tracks.PlaylistEditor;
import net.sparktank.morrigan.gui.editors.tracks.RemoteLibraryEditor;
import net.sparktank.morrigan.gui.views.ViewMediaExplorer;
import net.sparktank.morrigan.model.explorer.MediaExplorerItem;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class CallMediaListEditor extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.handler.CallMediaListEditor";
	
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
				
				MediaItemListEditorInput<?> input;
				String editorId;
				
				if (item.type == MediaExplorerItem.ItemType.PLAYLIST) {
					try {
						input = EditorFactory.getMediaPlaylistInput(item.identifier);
						editorId = PlaylistEditor.ID;
					} catch (Exception e) {
						new MorriganMsgDlg(e).open();
						return null;
					}
					
				} else if (item.type == MediaExplorerItem.ItemType.LIBRARY) {
					try {
						input = EditorFactory.getMediaLibraryInput(item.identifier);
						editorId = LocalLibraryEditor.ID;
					} catch (Exception e) {
						new MorriganMsgDlg(e).open();
						return null;
					}
					
				} else if (item.type == MediaExplorerItem.ItemType.REMOTELIBRARY) {
					try {
						input = EditorFactory.getRemoteMediaLibraryInput(item.identifier);
						editorId = RemoteLibraryEditor.ID;
					} catch (Exception e) {
						new MorriganMsgDlg(e).open();
						return null;
					}
					
				} else if (item.type == MediaExplorerItem.ItemType.LOCALGALLERY) {
					try {
						input = EditorFactory.getGalleryInput(item.identifier);
						editorId = LocalGalleryEditor.ID;
					} catch (Exception e) {
						new MorriganMsgDlg(e).open();
						return null;
					}
					
				} else {
					new MorriganMsgDlg("TODO: show " + item.identifier).open();
					return null;
				}
				
				try {
					page.openEditor(input, editorId);
					
				} catch (Exception e) {
					new MorriganMsgDlg(e).open();
					return null;
				}
			}
		}
		return null;
	}

}
