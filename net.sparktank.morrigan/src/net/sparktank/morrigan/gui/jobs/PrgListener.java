package net.sparktank.morrigan.gui.jobs;

import net.sparktank.morrigan.gui.helpers.ConsoleHelper;
import net.sparktank.morrigan.helpers.ErrorHelper;
import net.sparktank.morrigan.model.tasks.TaskEventListener;

import org.eclipse.core.runtime.IProgressMonitor;

public class PrgListener implements TaskEventListener {
	
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
	public void logError(String topic, String s, Throwable t) {
		String causeTrace = ErrorHelper.getCauseTrace(t);
		ConsoleHelper.appendToConsole(topic, s.concat("\n".concat(causeTrace)));
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
	
}