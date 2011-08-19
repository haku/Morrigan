package com.vaguehope.morrigan.gui.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;

import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.gui.editors.MediaItemListEditor;
import com.vaguehope.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import com.vaguehope.morrigan.gui.jobs.TaskJob;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
import com.vaguehope.morrigan.model.media.impl.MediaFactoryImpl;
import com.vaguehope.morrigan.model.tasks.IMorriganTask;

/**
 * 
 * @param <T> the type of the source list.
 */
public class CopyToLocalMmdbAction<T extends IMediaItem> extends Action {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaItemListEditor<?,T> fromEd;
	private final IEditorReference toEd;
	
	public CopyToLocalMmdbAction (MediaItemListEditor<?,T> fromEd, IEditorReference toEd) {
		super(toEd.getName(), Activator.getImageDescriptor("icons/db.gif"));
		this.fromEd = fromEd;
		this.toEd = toEd;
	}
	
	@Override
	public void run() {
		super.run();
		
		IWorkbenchPart toPart = this.toEd.getPart(true);
		
		if (this.fromEd != null && toPart != null && toPart instanceof LocalMixedMediaDbEditor) {
			IMediaItemList<T> fromList = this.fromEd.getMediaList();
			LocalMixedMediaDbEditor toMmdbEd = (LocalMixedMediaDbEditor) toPart;
			ILocalMixedMediaDb toMmdb = toMmdbEd.getMediaList();
			
			IMorriganTask task = MediaFactoryImpl.get().getNewCopyToLocalMmdbTask(fromList, this.fromEd.getSelectedItems(), toMmdb);
			TaskJob job = new TaskJob(task, Display.getCurrent());
			job.schedule();
		}
		else {
			throw new IllegalArgumentException("part is null or is not LocalMixedMediaDbEditor.");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}