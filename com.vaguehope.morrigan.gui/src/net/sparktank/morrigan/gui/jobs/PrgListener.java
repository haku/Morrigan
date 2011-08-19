package net.sparktank.morrigan.gui.jobs;

import net.sparktank.morrigan.gui.helpers.ConsoleHelper;

import org.eclipse.core.runtime.IProgressMonitor;

import com.vaguehope.morrigan.model.tasks.TaskEventListener;
import com.vaguehope.morrigan.util.ErrorHelper;

public class PrgListener implements TaskEventListener {
	
	private final IProgressMonitor monitor;

	public PrgListener (IProgressMonitor monitor) {
		this.monitor = monitor;
	}
	
	@Override
	public void onStart() {
//		ConsoleHelper.showConsole();
	}
	
	@Override
	public void logMsg(String topic, String s) {
		ConsoleHelper.appendToConsole(topic, s);
	}
	
	@Override
	public void logError(String topic, String s, Throwable t) {
		String causeTrace = ErrorHelper.getCauseTrace(t);
		ConsoleHelper.appendToConsole(topic, s.concat("\n".concat(causeTrace)));
	}

	@Override
	public void beginTask(String name, int totalWork) {
		this.monitor.beginTask(name, totalWork);
	}

	@Override
	public void done() {
		this.monitor.done();
	}

	@Override
	public boolean isCanceled() {
		return this.monitor.isCanceled();
	}

	@Override
	public void subTask(String name) {
		this.monitor.subTask(name);
	}

	@Override
	public void worked(int work) {
		this.monitor.worked(work);
	}
	
}
