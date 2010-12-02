package net.sparktank.morrigan.gui.actions;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.jobs.TaskJob;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IMediaItemDb;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.impl.MediaFactoryImpl;
import net.sparktank.morrigan.model.tasks.IMorriganTask;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

public class DbUpdateAction extends Action implements IWorkbenchAction{
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private IMediaItemDb<?,?,?> itemDb = null;
	
	public DbUpdateAction () {
		super();
	}
	
	public DbUpdateAction (IMediaItemDb<?,?,?> library) {
		super();
		this.itemDb = library;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setMediaDb (IMediaItemDb<?,?,?> itemDb) {
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
		
		if (this.itemDb instanceof ILocalMixedMediaDb) {
			ILocalMixedMediaDb lmmdb = (ILocalMixedMediaDb) this.itemDb;
			task = MediaFactoryImpl.get().getLocalMixedMediaDbUpdateTask(lmmdb);
		}
		else if (this.itemDb instanceof IRemoteMixedMediaDb) {
			IRemoteMixedMediaDb rmmdb = (IRemoteMixedMediaDb) this.itemDb;
			task = MediaFactoryImpl.get().getRemoteMixedMediaDbUpdateTask(rmmdb);
		}
		else {
			throw new IllegalArgumentException("TODO: Update has not been implemented for this type of DB '"+this.itemDb.getType()+"'.");
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
