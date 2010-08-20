package net.sparktank.morrigan.gui.handler;

import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import net.sparktank.morrigan.gui.editors.tracks.LocalLibraryEditor;
import net.sparktank.morrigan.gui.editors.tracks.MediaTrackListEditor;
import net.sparktank.morrigan.gui.editors.tracks.PlaylistEditor;
import net.sparktank.morrigan.gui.views.AbstractPlayerView;
import net.sparktank.morrigan.gui.views.ViewControls;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrackList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public class CallPlayMedia extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.handler.CallPlayMedia";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// FIXME work out how to pass parameters correctly.
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		IEditorPart activeEditor = page.getActiveEditor();
		
		if (activeEditor instanceof PlaylistEditor || activeEditor instanceof LocalLibraryEditor) {
			MediaTrackListEditor<IMediaTrackList<IMediaTrack>, IMediaTrack> mediaListEditor = (MediaTrackListEditor<IMediaTrackList<IMediaTrack>, IMediaTrack>) activeEditor;
			IMediaTrackList<IMediaTrack> mediaList = mediaListEditor.getMediaList();
			IMediaTrack selectedItem = mediaListEditor.getSelectedItem();
			playItem(page, mediaList, selectedItem);
		}
		else if (activeEditor instanceof LocalMixedMediaDbEditor) {
			LocalMixedMediaDbEditor lmmdbe = (LocalMixedMediaDbEditor) activeEditor;
			IMediaTrackList<? extends IMediaTrack> mediaList = lmmdbe.getMediaList();
			IMediaTrack selectedItem = lmmdbe.getSelectedItem();
			playItem(page, mediaList, selectedItem);
		}
		else {
			new MorriganMsgDlg("Error: invalid active editor.").open();
		}
		
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private void playItem(IWorkbenchPage page, IMediaTrackList<? extends IMediaTrack> mediaList, IMediaTrack selectedItem) {
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
			playerView.getPlayer().loadAndStartPlaying(mediaList, selectedItem);
		
		} else {
			new MorriganMsgDlg("Error: failed to find an AbstractPlayerView.").open();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
