package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.editors.MediaItemListEditor;
import net.sparktank.morrigan.gui.editors.tracks.PlaylistEditor;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;

public class AddToPlaylistAction extends Action {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaItemListEditor<?,?> fromEd;
	private final IEditorReference toEd;
	
	/**
	 * 
	 * @param editor The editor for the playlist we are going to add
	 * the selected items to.
	 */
	public AddToPlaylistAction (MediaItemListEditor<?,?> fromEd, IEditorReference toEd) {
		super(toEd.getName(), Activator.getImageDescriptor("icons/playlist.gif"));
		this.fromEd = fromEd;
		this.toEd = toEd;
	}
	
	@Override
	public void run() {
		super.run();
		
		IWorkbenchPart toPart = this.toEd.getPart(true);
		
		if (this.fromEd != null && toPart != null && toPart instanceof PlaylistEditor) {
			PlaylistEditor plPart = (PlaylistEditor) toPart;
			for (IMediaItem track : this.fromEd.getSelectedItems()) {
				plPart.addItem(track.getFilepath());
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}