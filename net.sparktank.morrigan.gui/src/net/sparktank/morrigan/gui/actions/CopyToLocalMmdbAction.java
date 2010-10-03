package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.editors.MediaItemListEditor;
import net.sparktank.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDb;
import net.sparktank.morrigan.model.media.interfaces.IMediaItem;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;

public class CopyToLocalMmdbAction extends Action {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaItemListEditor<?,?> fromEd;
	private final IEditorReference toEd;
	
	public CopyToLocalMmdbAction (MediaItemListEditor<?,?> fromEd, IEditorReference toEd) {
		super(toEd.getName(), Activator.getImageDescriptor("icons/db.gif"));
		this.fromEd = fromEd;
		this.toEd = toEd;
	}
	
	@Override
	public void run() {
		super.run();
		
		IWorkbenchPart toPart = this.toEd.getPart(true);
		
		if (this.fromEd != null && toPart != null && toPart instanceof LocalMixedMediaDbEditor) {
			LocalMixedMediaDbEditor ed = (LocalMixedMediaDbEditor) toPart;
			LocalMixedMediaDb ml = ed.getMediaList();
			
			StringBuilder sb = new StringBuilder();
			sb.append("TODO: add files to '"+ml.getListName()+"'.");
			for (IMediaItem item : this.fromEd.getSelectedItems()) {
				sb.append("\n");
				sb.append(item.getTitle());
			}
			new MorriganMsgDlg(sb.toString()).open();
		}
		else {
			throw new IllegalArgumentException("part is null or is not LocalMixedMediaDbEditor.");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}