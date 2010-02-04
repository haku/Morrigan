package net.sparktank.morrigan.library;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.actions.MorriganAction;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Event;

public class LibraryUpdateTask extends MorriganAction {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getText() { return "Update library"; }
	
	@Override
	public String getId() { return "updatelibrary"; }
	
	@Override
	public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
		// TODO choose icon.
		return Activator.getImageDescriptor("icons/play.gif");
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void runWithEvent(Event event) {
		Job job = new Job("Library scan"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("resting.", 100);
				
				for (int i = 0; i<100; i++) {
					monitor.worked(1);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					if (monitor.isCanceled()) {
						System.out.println("Task was canceled desu~.");
						break;
					}
				}
				
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
