package com.vaguehope.morrigan.gui.handler;

import java.util.ArrayList;


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.editors.mmdb.MixedMediaListEditor;
import com.vaguehope.morrigan.gui.editors.tracks.MediaTrackListEditor;
import com.vaguehope.morrigan.gui.views.AbstractPlayerView;
import com.vaguehope.morrigan.gui.views.ViewControls;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.PlayItem;

public class AddToQueue  extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.handler.AddToQueue";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// FIXME work out how to pass parameters correctly.
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		
		IViewPart findView = page.findView(ViewControls.ID);
		if (findView == null) {
			try {
				findView = page.showView(ViewControls.ID);
			} catch (PartInitException e) {
				new MorriganMsgDlg(e).open();
			}
		}
		
		if (findView != null) {
			IMediaTrackList<? extends IMediaTrack> list;
			ArrayList<? extends IMediaTrack> selectedTracks;
			
			IEditorPart activeEditor = page.getActiveEditor();
			if (activeEditor instanceof MediaTrackListEditor<?,?>) {
				MediaTrackListEditor<?,?> editor = (MediaTrackListEditor<?,?>) activeEditor;
				list = editor.getMediaList();
				selectedTracks = editor.getSelectedItems();
			}
			else if (activeEditor instanceof MixedMediaListEditor<?,?>) {
				MixedMediaListEditor<?,?> editor = (MixedMediaListEditor<?,?>) activeEditor;
				list = editor.getMediaList();
				selectedTracks = editor.getSelectedItems();
			}
			else {
				new MorriganMsgDlg("Error: invalid active editor.").open();
				return null;
			}
			
			AbstractPlayerView playerView = (AbstractPlayerView) findView;
			
			if (selectedTracks != null) {
				for (IMediaTrack track : selectedTracks) {
					if (track.isPlayable()) { // Don't queue things we can't play.
						PlayItem item = new PlayItem(list, track);
						playerView.getPlayer().addToQueue(item);
					}
				}
			}
		}
		else {
			new MorriganMsgDlg("Error: failed to find ViewControls.").open();
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
