package net.sparktank.morrigan.gui.handler;

import java.util.ArrayList;

import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.MediaTrackListEditor;
import net.sparktank.morrigan.gui.views.AbstractPlayerView;
import net.sparktank.morrigan.gui.views.ViewControls;
import net.sparktank.morrigan.model.playlist.PlayItem;
import net.sparktank.morrigan.model.tracks.MediaTrack;

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
	
	public static final String ID = "net.sparktank.morrigan.gui.handler.AddToQueue";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// FIXME work out how to pass parameters correctly.
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		IEditorPart activeEditor = page.getActiveEditor();
		
		if (activeEditor instanceof MediaTrackListEditor<?,?>) {
			MediaTrackListEditor<?,?> mediaListEditor = (MediaTrackListEditor<?,?>) activeEditor;
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
				
				ArrayList<? extends MediaTrack> selectedTracks = mediaListEditor.getSelectedTracks();
				if (selectedTracks != null) {
					for (MediaTrack track : selectedTracks) {
						PlayItem item = new PlayItem(mediaListEditor.getMediaList(), track);
						playerView.getPlayer().addToQueue(item);
					}
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
