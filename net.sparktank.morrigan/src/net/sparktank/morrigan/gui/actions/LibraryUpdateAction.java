package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.jobs.TaskJob;
import net.sparktank.morrigan.model.MediaItemDb;
import net.sparktank.morrigan.model.tracks.library.local.LocalLibraryUpdateTask;
import net.sparktank.morrigan.model.tracks.library.local.LocalMediaLibrary;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/*
 * TODO refactor to be LocalDbUpdateAction (or something like that).
 */
public class LibraryUpdateAction extends Action implements IWorkbenchAction{
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MediaItemDb<?,?,?> library = null;
	
	public LibraryUpdateAction () {
		super();
	}
	
	public LibraryUpdateAction (MediaItemDb<?,?,?> library) {
		super();
		this.library = library;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setMediaLibrary (MediaItemDb<?,?,?> library) {
		this.library = library;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { 
		if (this.library != null) {
			return "Update " + this.library.getListName();
		}
		
		return "Update library";
	}
	
	@Override
	public String getId() { return "updatelibrary"; }
	
	@Override
	public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
		// TODO choose icon.
		return Activator.getImageDescriptor("icons/search.gif");
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run() {
		if (this.library==null) {
			new MorriganMsgDlg("No library selected desu~.").open();
			return;
		}
		
		LocalLibraryUpdateTask task;
		
		if (this.library instanceof LocalMediaLibrary) {
			LocalMediaLibrary lml = (LocalMediaLibrary) this.library;
			task = LocalLibraryUpdateTask.FACTORY.manufacture(lml);
		}
		else {
			throw new IllegalArgumentException("TODO: Only update for LocalMediaLibrary has been implemented.");
		}
		
		if (task != null) {
			TaskJob job = new TaskJob(task, Display.getCurrent());
			job.schedule();
		
		} else {
			new MorriganMsgDlg("An update is already running for this library.").open();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void dispose() {/* UNUSED */}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
