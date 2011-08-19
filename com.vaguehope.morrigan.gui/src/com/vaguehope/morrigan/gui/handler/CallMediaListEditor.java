package com.vaguehope.morrigan.gui.handler;


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.editors.EditorFactory;
import com.vaguehope.morrigan.gui.editors.MediaItemListEditorInput;
import com.vaguehope.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import com.vaguehope.morrigan.gui.editors.mmdb.RemoteMixedMediaDbEditor;
import com.vaguehope.morrigan.gui.views.ViewMediaExplorer;
import com.vaguehope.morrigan.model.media.MediaListReference;

public class CallMediaListEditor extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "com.vaguehope.morrigan.gui.handler.CallMediaListEditor";
	
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
				MediaListReference item = (MediaListReference) obj;
				
				MediaItemListEditorInput<?> input;
				String editorId;
				
				if (item.getType() == MediaListReference.MediaListType.LOCALMMDB) {
					try {
						input = EditorFactory.getMmdbInput(item.getIdentifier());
						editorId = LocalMixedMediaDbEditor.ID;
					} catch (Exception e) {
						new MorriganMsgDlg(e).open();
						return null;
					}
				}
				else if (item.getType() == MediaListReference.MediaListType.REMOTEMMDB) {
					try {
						input = EditorFactory.getRemoteMmdbInput(item.getIdentifier());
						editorId = RemoteMixedMediaDbEditor.ID;
					} catch (Exception e) {
						new MorriganMsgDlg(e).open();
						return null;
					}
				}
				else {
					new MorriganMsgDlg("TODO: show " + item.getIdentifier()).open();
					return null;
				}
				
				try {
					page.openEditor(input, editorId);
				}
				catch (Exception e) {
					new MorriganMsgDlg(e).open();
					return null;
				}
			}
		}
		return null;
	}

}
