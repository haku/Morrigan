package net.sparktank.morrigan.gui.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sparktank.morrigan.gui.dialogs.JumpToDlg;
import net.sparktank.morrigan.gui.dialogs.RunnableDialog;
import net.sparktank.morrigan.gui.editors.EditorFactory;
import net.sparktank.morrigan.gui.editors.MediaItemDbEditorInput;
import net.sparktank.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import net.sparktank.morrigan.gui.views.ViewControls;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackDb;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.IPlayerLocal;
import com.vaguehope.morrigan.player.PlayItem;

public class JumpToAction extends Action {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final IWorkbenchWindow workbenchWindow;
	private final IMediaTrackList<? extends IMediaTrack> list;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public JumpToAction (IWorkbenchWindow workbenchWindow) {
		this(workbenchWindow, null);
	}
	
	public JumpToAction (IWorkbenchWindow workbenchWindow, IMediaTrackList<? extends IMediaTrack> list) {
		super("Jump to...");
		this.workbenchWindow = workbenchWindow;
		this.list = list;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run() {
		final IViewPart view = this.workbenchWindow.getActivePage().findView(ViewControls.ID);
		if (!(view instanceof ViewControls)) {
			this.workbenchWindow.getShell().getDisplay().asyncExec(new RunnableDialog("Failed to find ViewControls in workbench window."));
			return;
		}
		final ViewControls viewControls = (ViewControls) view;
		final IPlayerLocal player;
		player = viewControls.getPlayer();
		
		IMediaTrackList<? extends IMediaTrack> currentList = this.list != null ? this.list : player.getCurrentList();
		if (currentList == null || !(currentList instanceof IMediaTrackDb<?,?,?>)) return;
		IMediaTrackDb<?,?,?> currentDb = (IMediaTrackDb<?,?,?>) currentList;
		
		JumpToDlg dlg = new JumpToDlg(this.workbenchWindow.getShell(), (IMediaTrackDb<?,?,?>) currentList);
		dlg.open();
		IMediaTrack item = dlg.getReturnItem();
		if (item != null) {
			if ((dlg.getKeyMask() & SWT.ALT) != 0 && dlg.getReturnList() != null) {
				if ((dlg.getKeyMask() & SWT.SHIFT) != 0 && (dlg.getKeyMask() & SWT.CONTROL) != 0) {
					String filter = dlg.getReturnFilter();
					try {
						MediaItemDbEditorInput input = EditorFactory.getMmdbInput(currentDb.getDbPath(), filter);
						this.workbenchWindow.getActivePage().openEditor(input, LocalMixedMediaDbEditor.ID);
					}
					catch (MorriganException e) {
						this.workbenchWindow.getShell().getDisplay().asyncExec(new RunnableDialog(e));
					} catch (PartInitException e) {
						this.workbenchWindow.getShell().getDisplay().asyncExec(new RunnableDialog(e));
					}
				}
				else {
					List<IMediaTrack> shuffeledList = new ArrayList<IMediaTrack>(dlg.getReturnList());
					Collections.shuffle(shuffeledList);
					for (IMediaTrack track : shuffeledList) {
						player.addToQueue(new PlayItem(currentList, track));
					}
				}
			}
			else if ((dlg.getKeyMask() & SWT.SHIFT) != 0 && (dlg.getKeyMask() & SWT.CONTROL) != 0) {
				// TODO extract revealItemInLists() and do not go via ViewControls.
				viewControls.revealItemInLists(currentList, item);
			}
			else if ((dlg.getKeyMask() & SWT.SHIFT) != 0 || (dlg.getKeyMask() & SWT.CONTROL) != 0) {
				player.addToQueue(new PlayItem(currentList, item));
			}
			else {
				player.loadAndStartPlaying(currentList, item);
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
