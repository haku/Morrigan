package net.sparktank.morrigan.handler;

import net.sparktank.morrigan.editors.MediaListEditor;
import net.sparktank.morrigan.editors.MediaListEditorInput;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public class CallMediaListEditor extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
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
					MediaPlaylist playList = new MediaPlaylist(item.identifier);
					MediaListEditorInput input = new MediaListEditorInput(playList);
					try {
						page.openEditor(input, MediaListEditor.ID);
					} catch (PartInitException e) {
						System.out.println(e.getStackTrace());
					}
					
				} else {
					System.out.println("TODO: show " + item.identifier);
				}
				
				// TODO convert MediaExplorer objects to something more useful than string.
				// TODO look up what was double clicked on.
				// TODO display the appropriate editor.
				
				// CODE from example.
				
//				MediaList mediaList = (MediaList) obj;
//				MediaListEditorInput input = new MediaListEditorInput(mediaList);
//				try {
//					page.openEditor(input, MediaListEditor.ID);
//					
//				} catch (PartInitException e) {
//					System.out.println(e.getStackTrace());
//				}
			}
		}
		return null;
	}

}
