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
	
	/**
	 * TODO use monitor.worked(1);
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Updating library", IProgressMonitor.UNKNOWN);
		
		monitor.subTask("Scanning sources");
		
		List<String> sources = null;
		try {
			sources = library.getSources();
		} catch (DbException e) {
			e.printStackTrace();
		}
		
		if (sources!=null) {
			for (String source : sources) {
				monitor.subTask("Scanning "+source);
				
				Stack<File> dirStack = new Stack<File>();
				dirStack.push(new File(source));
				
				while (!dirStack.isEmpty()) {
					File dirItem = dirStack.pop();
					File[] listFiles = dirItem.listFiles();
					
					for (File file : listFiles) {
						if (file.isDirectory()) {
							dirStack.push(file);
							
						} else if (file.isFile()) {
							try {
								// TODO check file type.
								library.addFile(file);
							} catch (DbException e) {
								// FIXME log this somewhere useful.
								e.printStackTrace();
							}
							
						}
						
						if (monitor.isCanceled()) break;
					}
					
					if (monitor.isCanceled()) {
						System.out.println("Task was canceled desu~.");
						break;
					}
				}
			}
		}
		
//		TODO refresh library view. (mark dirty / event trigger in editor?)
		
		monitor.done();
		return Status.OK_STATUS;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
