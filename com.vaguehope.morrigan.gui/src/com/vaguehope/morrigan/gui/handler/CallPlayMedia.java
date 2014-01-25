package com.vaguehope.morrigan.gui.handler;


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import com.vaguehope.morrigan.gui.views.AbstractPlayerView;
import com.vaguehope.morrigan.gui.views.ViewControls;
import com.vaguehope.morrigan.gui.views.ViewPicture;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.player.PlayItem;

public class CallPlayMedia extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "com.vaguehope.morrigan.gui.handler.CallPlayMedia";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// FIXME work out how to pass parameters correctly.
		
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		IWorkbenchPage page = window.getActivePage();
		IEditorPart activeEditor = page.getActiveEditor();
		
		if (activeEditor instanceof LocalMixedMediaDbEditor) {
			LocalMixedMediaDbEditor lmmdbe = (LocalMixedMediaDbEditor) activeEditor;
			ILocalMixedMediaDb mediaList = lmmdbe.getMediaList();
			IMixedMediaItem selectedItem = lmmdbe.getSelectedItem();
			if (selectedItem.getMediaType() == MediaType.TRACK) { 
				playItem(page, mediaList, selectedItem);
			}
			else if (selectedItem.getMediaType() == MediaType.PICTURE) {
				try {
					IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ViewPicture.ID);
					ViewPicture picView = (ViewPicture) view;
					picView.setInput(mediaList, selectedItem);
				}
				catch (PartInitException e) {
					throw new ExecutionException("", e); // TODO add msg.
				}
			}
			else {
				new MorriganMsgDlg("Error: don't know how to play the type '"+selectedItem.getMediaType()+"'.").open();
			}
		}
		else {
			new MorriganMsgDlg("Error: invalid active editor.").open();
		}
		
		return null;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static void playItem (IWorkbenchPage page, IMediaTrackList<? extends IMediaTrack> mediaList) {
		playItem(page, mediaList, null, false);
	}
	
	public static void playItem (IWorkbenchPage page, IMediaTrackList<? extends IMediaTrack> mediaList, boolean addToQueue) {
		playItem(page, mediaList, null, addToQueue);
	}
	
	public static void playItem (IWorkbenchPage page, IMediaTrackList<? extends IMediaTrack> mediaList, IMediaTrack selectedItem) {
		playItem(page, mediaList, selectedItem, false);
	}
	
	public static void playItem (IWorkbenchPage page, IMediaTrackList<? extends IMediaTrack> mediaList, IMediaTrack selectedItem, boolean addToQueue) {
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
			if (selectedItem == null) {
				if (addToQueue) {
					playerView.getPlayer().getQueue().addToQueue(new PlayItem(mediaList, null));
				}
				else {
					playerView.getPlayer().loadAndStartPlaying(mediaList);
				}
			}
			else {
				if (addToQueue) {
					playerView.getPlayer().getQueue().addToQueue(new PlayItem(mediaList, selectedItem));
				}
				else {
					playerView.getPlayer().loadAndStartPlaying(mediaList, selectedItem);
				}
			}
		
		} else {
			new MorriganMsgDlg("Error: failed to find an AbstractPlayerView.").open();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
