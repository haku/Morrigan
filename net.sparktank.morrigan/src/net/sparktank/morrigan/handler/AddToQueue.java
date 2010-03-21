package net.sparktank.morrigan.handler;

import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.editors.MediaListEditor;
import net.sparktank.morrigan.model.media.MediaItem;
import net.sparktank.morrigan.model.media.PlayItem;
import net.sparktank.morrigan.views.AbstractPlayerView;
import net.sparktank.morrigan.views.ViewControls;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public class AddToQueue  extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.handler.AddToQueue";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// FIXME work out how to pass paramaters correctly.
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		IEditorPart activeEditor = page.getActiveEditor();
		
		if (activeEditor instanceof MediaListEditor<?>) {
			MediaListEditor<?> mediaListEditor = (MediaListEditor<?>) activeEditor;
			AbstractPlayerView playerView;
			IViewPart findView = page.findView(ViewControls.ID);
			
			if (findView == null) {
				try {
					findView = page.showView(ViewControls.ID);
				} catch (PartInitException e) {
					new MorriganMsgDlg(e).open();
				}
			}
			
			if (findView != null) {
				playerView = (AbstractPlayerView) findView;
				
				for (MediaItem track : mediaListEditor.getSelectedTracks()) {
					PlayItem item = new PlayItem(mediaListEditor.getMediaList(), track);
					playerView.addToQueue(item);
				}
				
			} else {
				new MorriganMsgDlg("Error: failed to find an AbstractPlayerView.").open();
			}
			
		} else {
			new MorriganMsgDlg("Error: invalid active editor.").open();
		}
		
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
