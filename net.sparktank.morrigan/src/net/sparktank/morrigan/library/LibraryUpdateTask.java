package net.sparktank.morrigan.library;

import java.io.File;
import java.util.List;
import java.util.Stack;

import net.sparktank.morrigan.model.media.MediaLibrary;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * FIXME prevent concurrent executions of this task.
 */
public class LibraryUpdateTask extends Job {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MediaLibrary library;
	
	public LibraryUpdateTask (MediaLibrary library) {
		super("Update " + library.getListName());
		this.library = library;
		setUser(true);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Updating library...", 100);
		
		monitor.subTask("Scanning sources...");
		
		List<String> sources = null;
		try {
			sources = library.getSources();
		} catch (DbException e) {
			e.printStackTrace();
		}
		
		if (sources!=null) {
			for (String source : sources) {
				Stack<File> dirStack = new Stack<File>();
				dirStack.push(new File(source));
				
				while (!dirStack.isEmpty()) {
					File dirItem = dirStack.pop();
					File[] listFiles = dirItem.listFiles();
					
					for (File file : listFiles) {
						if (file.isDirectory()) {
							dirStack.push(file);
							
						} else if (file.isFile()) {
							// TODO now add this file to the library.
							
						}
					}
				}
			}
		}
		
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
