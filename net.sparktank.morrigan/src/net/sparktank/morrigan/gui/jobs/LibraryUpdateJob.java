package net.sparktank.morrigan.gui.jobs;

import net.sparktank.morrigan.gui.helpers.ConsoleHelper;
import net.sparktank.morrigan.model.library.LocalLibraryUpdateTask;
import net.sparktank.morrigan.model.library.LocalLibraryUpdateTask.TaskEventListener;
import net.sparktank.morrigan.model.library.LocalLibraryUpdateTask.TaskResult;
import net.sparktank.morrigan.model.library.LocalLibraryUpdateTask.TaskResult.TaskOutcome;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class LibraryUpdateJob extends Job {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final LocalLibraryUpdateTask libraryUpdateTask;

	public LibraryUpdateJob (LocalLibraryUpdateTask libraryUpdateTask) {
		super("Update " + libraryUpdateTask.getLibrary().getListName());
		this.libraryUpdateTask = libraryUpdateTask;
		setUser(true);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -	
	
	static private class PrgListener implements TaskEventListener {
		
		private final IProgressMonitor monitor;

		public PrgListener (IProgressMonitor monitor) {
			this.monitor = monitor;
		}
		
		@Override
		public void onStart() {
			ConsoleHelper.showConsole();
		}
		
		@Override
		public void logMsg(String topic, String s) {
			ConsoleHelper.appendToConsole(topic, s);
		}

		@Override
		public void beginTask(String name, int totalWork) {
			monitor.beginTask(name, totalWork);
		}

		@Override
		public void done() {
			monitor.done();
		}

		@Override
		public boolean isCanceled() {
			return monitor.isCanceled();
		}

		@Override
		public void subTask(String name) {
			monitor.subTask(name);
		}

		@Override
		public void worked(int work) {
			monitor.worked(work);
		}
		
	};
	
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
	
	static public class FailStatus implements IStatus {
		
		private final String message;
		private final Throwable e;

		public FailStatus (String message, Throwable e) {
			this.message = message;
			this.e = e;
		}
		
		@Override
		public IStatus[] getChildren() {
			return null;
		}
		
		@Override
		public int getCode() {
			return 0;
		}
		
		@Override
		public Throwable getException() {
			return e;
		}
		
		@Override
		public String getMessage() {
			return message;
		}
		
		@Override
		public String getPlugin() {
			return null;
		}
		
		@Override
		public int getSeverity() {
			return 0;
		}
		
		@Override
		public boolean isMultiStatus() {
			return false;
		}
		
		@Override
		public boolean isOK() {
			return false;
		}
		
		@Override
		public boolean matches(int severityMask) {
			return false;
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -	
}
