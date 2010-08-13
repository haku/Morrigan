package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.editors.tracks.PlaylistEditor;
import net.sparktank.morrigan.model.media.impl.MediaItem;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;

public class AddToPlaylistAction extends Action {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final IEditorReference editor;
	
	/**
	 * 
	 * @param editor The editor for the playlist we are going to add
	 * the selected items to.
	 */
	public AddToPlaylistAction (IEditorReference editor) {
		super(editor.getName(), Activator.getImageDescriptor("icons/playlist.gif"));
		editor.getTitleImage();
		this.editor = editor;
	}
	
	@Override
	public void run() {
		super.run();
		
		IWorkbenchPart part = this.editor.getPart(false);
		
		if (part != null && part instanceof PlaylistEditor) {
			PlaylistEditor plPart = (PlaylistEditor) part;
			for (MediaItem track : plPart.getSelectedItems()) {
				plPart.addItem(track.getFilepath());
			}
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}