package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.jobs.TaskJob;
import net.sparktank.morrigan.model.media.impl.MediaItemDb;
import net.sparktank.morrigan.model.pictures.gallery.LocalGallery;
import net.sparktank.morrigan.model.pictures.gallery.LocalGalleryUpdateTask;
import net.sparktank.morrigan.model.tasks.IMorriganTask;
import net.sparktank.morrigan.model.tracks.library.local.LocalLibraryUpdateTask;
import net.sparktank.morrigan.model.tracks.library.local.LocalMediaLibrary;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

public class DbUpdateAction extends Action implements IWorkbenchAction{
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaItemDb<?,?> itemDb = null;
	
	public DbUpdateAction () {
		super();
	}
	
	public DbUpdateAction (MediaItemDb<?,?> library) {
		super();
		this.itemDb = library;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setMediaDb (MediaItemDb<?,?> itemDb) {
		this.itemDb = itemDb;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { 
		if (this.itemDb != null) {
			return "Update " + this.itemDb.getListName();
		}
		
		return "Update DB";
	}
	
	@Override
	public String getId() { return "updatedb"; }
	
	@Override
	public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
		// TODO choose icon.
		return Activator.getImageDescriptor("icons/search.gif");
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run() {
		if (this.itemDb == null) {
			new MorriganMsgDlg("No DB selected desu~.").open();
			return;
		}
		
		IMorriganTask task;
		
		if (this.itemDb instanceof LocalMediaLibrary) {
			LocalMediaLibrary lml = (LocalMediaLibrary) this.itemDb;
			task = LocalLibraryUpdateTask.FACTORY.manufacture(lml);
		}
		else if (this.itemDb instanceof LocalGallery) {
			LocalGallery lml = (LocalGallery) this.itemDb;
			task = LocalGalleryUpdateTask.FACTORY.manufacture(lml);
		}
		else {
			throw new IllegalArgumentException("TODO: Update has not been implemented for this type of DB.");
		}
		
		if (task != null) {
			TaskJob job = new TaskJob(task, Display.getCurrent());
			job.schedule();
		}
		else {
			new MorriganMsgDlg("An update is already running for this DB.").open();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void dispose() {/* UNUSED */}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
