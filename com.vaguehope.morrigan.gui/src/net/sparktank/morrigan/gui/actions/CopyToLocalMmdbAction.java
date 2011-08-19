package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.editors.MediaItemListEditor;
import net.sparktank.morrigan.gui.editors.mmdb.LocalMixedMediaDbEditor;
import net.sparktank.morrigan.gui.jobs.TaskJob;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaItem;
import net.sparktank.morrigan.model.media.IMediaItemList;
import net.sparktank.morrigan.model.media.impl.MediaFactoryImpl;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;

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