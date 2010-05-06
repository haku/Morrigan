package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.model.library.LocalLibraryUpdateTask;
import net.sparktank.morrigan.model.library.LocalMediaLibrary;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import net.sparktank.morrigan.gui.jobs.*;

public class LibraryUpdateAction extends Action implements IWorkbenchAction{
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private LocalMediaLibrary library = null;
	
	public LibraryUpdateAction () {
		super();
	}
	
	public LibraryUpdateAction (LocalMediaLibrary library) {
		super();
		this.library = library;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setMediaLibrary (LocalMediaLibrary library) {
		this.library = library;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { 
		if (library != null) {
			return "Update " + library.getListName();
		} else {
			return "Update library";
		}
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
		if (library==null) {
			new MorriganMsgDlg("No library selected desu~.").open();
			return;
		}
		
		LocalLibraryUpdateTask task = LocalLibraryUpdateTask.factory(library);
		if (task != null) {
			LibraryUpdateJob job = new LibraryUpdateJob(task);
			job.schedule();
		
		} else {
			new MorriganMsgDlg("An update is already running for this library.").open();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void dispose() {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
