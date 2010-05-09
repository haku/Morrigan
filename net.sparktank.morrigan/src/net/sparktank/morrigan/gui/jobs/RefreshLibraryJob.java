package net.sparktank.morrigan.gui.jobs;

import net.sparktank.morrigan.model.library.RemoteMediaLibrary;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class RefreshLibraryJob extends Job {
	
	private final RemoteMediaLibrary mediaLibrary;
	
	public RefreshLibraryJob(RemoteMediaLibrary mediaLibrary) {
		super("Refresh ".concat(mediaLibrary.getListName()));
		this.mediaLibrary = mediaLibrary;
		setUser(true);
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		try {
			mediaLibrary.setTaskEventListener(new PrgListener(monitor));
			mediaLibrary.invalidateCache();
			mediaLibrary.reRead();
			return Status.OK_STATUS;
			
		} catch (Exception e) {
			// TODO show error msg?
			e.printStackTrace();
			return new FailStatus("Failed to refresh library.", e);
			
		} finally {
			mediaLibrary.setTaskEventListener(null);
		}
	}
	
}
