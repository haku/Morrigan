package net.sparktank.morrigan.gui.jobs;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.library.RemoteMediaLibrary;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class RefreshLibraryJob extends Job {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory stuff.
	
	public static class Factory extends RecyclingFactory<RefreshLibraryJob, RemoteMediaLibrary, Void, RuntimeException> {
		
		protected Factory() {
			super(false);
		}
		
		@Override
		protected boolean isValidProduct(RefreshLibraryJob product) {
			return !product.isFinished();
		}
		
		@Override
		protected RefreshLibraryJob makeNewProduct(RemoteMediaLibrary material) {
			return new RefreshLibraryJob(material);
		}
		
	}
	
	public static final Factory FACTORY = new Factory();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final RemoteMediaLibrary mediaLibrary;
	
	private volatile boolean isFinished = false;
	
	private RefreshLibraryJob(RemoteMediaLibrary mediaLibrary) {
		super("Refresh ".concat(mediaLibrary.getListName()));
		this.mediaLibrary = mediaLibrary;
		setUser(true);
	}
	
	public RemoteMediaLibrary getLibrary () {
		return mediaLibrary;
	}
	
	public boolean isFinished () {
		return isFinished;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
			isFinished = true;
			mediaLibrary.setTaskEventListener(null);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
