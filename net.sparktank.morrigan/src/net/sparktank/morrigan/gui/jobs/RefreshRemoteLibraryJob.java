package net.sparktank.morrigan.gui.jobs;

import net.sparktank.morrigan.model.TaskResult;
import net.sparktank.morrigan.model.TaskResult.TaskOutcome;
import net.sparktank.morrigan.model.library.remote.RemoteLibraryUpdateTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * FIXME prevent multiple instances.
 */
public class RefreshRemoteLibraryJob extends Job {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final RemoteLibraryUpdateTask libraryUpdateTask;

	public RefreshRemoteLibraryJob (RemoteLibraryUpdateTask libraryUpdateTask) {
		super("Update " + libraryUpdateTask.getLibrary().getListName());
		this.libraryUpdateTask = libraryUpdateTask;
		setUser(true);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		TaskResult res = libraryUpdateTask.run(new PrgListener(monitor));
		
		if (res.getOutcome() == TaskOutcome.SUCCESS) {
			return Status.OK_STATUS;
			
		} else if (res.getOutcome() == TaskOutcome.CANCELED) {
			return Status.CANCEL_STATUS;
			
		} else {
			return new FailStatus(res.getErrMsg(), res.getErrThr());
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
