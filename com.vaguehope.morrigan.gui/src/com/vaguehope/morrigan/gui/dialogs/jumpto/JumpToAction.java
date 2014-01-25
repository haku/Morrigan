package com.vaguehope.morrigan.gui.dialogs.jumpto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import com.vaguehope.morrigan.gui.dialogs.RunnableDialog;
import com.vaguehope.morrigan.gui.editors.EditorFactory;
import com.vaguehope.morrigan.gui.editors.MediaItemDbEditorInput;
import com.vaguehope.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import com.vaguehope.morrigan.gui.views.ViewControls;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackDb;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.LocalPlayer;
import com.vaguehope.morrigan.player.PlayItem;

public class JumpToAction extends Action {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final IWorkbenchWindow workbenchWindow;
	private final IMediaTrackList<? extends IMediaTrack> list;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public JumpToAction (final IWorkbenchWindow workbenchWindow) {
		this(workbenchWindow, null);
	}

	public JumpToAction (final IWorkbenchWindow workbenchWindow, final IMediaTrackList<? extends IMediaTrack> list) {
		super("Jump to...");
		this.workbenchWindow = workbenchWindow;
		this.list = list;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void run () {
		final IViewPart view = this.workbenchWindow.getActivePage().findView(ViewControls.ID);
		if (!(view instanceof ViewControls)) {
			this.workbenchWindow.getShell().getDisplay().asyncExec(new RunnableDialog("Failed to find ViewControls in workbench window."));
			return;
		}
		final ViewControls viewControls = (ViewControls) view;
		final LocalPlayer player = viewControls.getPlayer();

		final IMediaTrackList<? extends IMediaTrack> currentList = this.list != null ? this.list : player.getCurrentList();
		if (currentList == null || !(currentList instanceof IMediaTrackDb<?, ?>)) return;
		final IMediaTrackDb<?, ?> currentDb = (IMediaTrackDb<?, ?>) currentList;

		final JumpToDlg dlg = new JumpToDlg(this.workbenchWindow.getShell(), (IMediaTrackDb<?, ?>) currentList);
		switch (dlg.open()) {
			case PLAY_NOW:
				player.loadAndStartPlaying(currentList, dlg.getReturnItem());
				break;
			case ENQUEUE:
				player.getQueue().addToQueue(new PlayItem(currentList, dlg.getReturnItem()));
				break;
			case REVEAL:
				// TODO extract revealItemInLists() and do not go via ViewControls.
				viewControls.revealItemInLists(currentList, dlg.getReturnItem());
				break;
			case SHUFFLE_AND_ENQUEUE:
				shuffleAndEnqueue(player, currentList, dlg);
				break;
			case OPEN_VIEW:
				openFilteredView(currentDb, dlg.getReturnFilter());
				break;
			default:
				break;
		}
	}

	private static void shuffleAndEnqueue (final LocalPlayer player, final IMediaTrackList<? extends IMediaTrack> currentList, final JumpToDlg dlg) {
		final List<IMediaTrack> shuffeledList = new ArrayList<IMediaTrack>(dlg.getSearchResults());
		Collections.shuffle(shuffeledList);
		for (final IMediaTrack track : shuffeledList) {
			player.getQueue().addToQueue(new PlayItem(currentList, track));
		}
	}

	private void openFilteredView (final IMediaTrackDb<?, ?> currentDb, final String filter) {
		try {
			MediaItemDbEditorInput input = EditorFactory.getMmdbInput(currentDb.getDbPath(), filter);
			this.workbenchWindow.getActivePage().openEditor(input, LocalMixedMediaDbEditor.ID);
		}
		catch (MorriganException e) {
			this.workbenchWindow.getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
		catch (PartInitException e) {
			this.workbenchWindow.getShell().getDisplay().asyncExec(new RunnableDialog(e));
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
