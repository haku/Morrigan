package net.sparktank.morrigan.handler;

import net.sparktank.morrigan.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.editors.MediaListEditor;
import net.sparktank.morrigan.views.AbstractPlayerView;
import net.sparktank.morrigan.views.ViewControls;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public class CallPlayMedia  extends AbstractHandler implements IHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.handler.CallPlayMedia";
	
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
			try {
//				playerView = (AbstractPlayerView) page.showView(ViewPlayer.ID);
				playerView = (AbstractPlayerView) page.showView(ViewControls.ID);
			} catch (PartInitException e) {
				new MorriganMsgDlg(e).open();
				return null;
			}
			playerView.loadAndStartPlaying(mediaListEditor.getEditedMediaList(), mediaListEditor.getSelectedTrack());
			
		} else {
			new MorriganMsgDlg("Error: invalid active editor.").open();
		}
		
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
