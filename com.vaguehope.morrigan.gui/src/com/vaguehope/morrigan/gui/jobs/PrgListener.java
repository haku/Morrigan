package com.vaguehope.morrigan.gui.jobs;


import org.eclipse.core.runtime.IProgressMonitor;

import com.vaguehope.morrigan.gui.helpers.ConsoleHelper;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.util.ErrorHelper;

/**
 * Adaptor class for sending status data to RCP IProgressMonitor.
 */
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
