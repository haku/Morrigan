package com.vaguehope.morrigan.asyncui;

import com.vaguehope.morrigan.model.tasks.TaskEventListener;
import com.vaguehope.morrigan.util.ErrorHelper;

/**
 * Adaptor class for sending status data to System.out.
 */
class SysOutPrgListener implements TaskEventListener {
	
	private final String logPrefix;
	private int totalWork = 0;
	private int workDone = 0;
	private boolean canceled;
	
	public SysOutPrgListener (String logPrefix) {
		this.logPrefix = logPrefix;
	}
	
	@Override
	public void logMsg(String topic, String s) {
		System.out.print("[");
		System.out.print(topic);
		System.out.print("] ");
		System.out.print(s);
		System.out.println();
	}
	
	@Override
	public void logError(String topic, String s, Throwable t) {
		String causeTrace = ErrorHelper.getCauseTrace(t);
		logMsg(topic, s.concat("\n".concat(causeTrace)));
	}
	
	@Override
	public void onStart() {/* UNUSED */}
	
	@Override
	public void beginTask(String name, @SuppressWarnings("hiding") int totalWork) {
		this.totalWork = totalWork;
		System.out.println("[" + this.logPrefix + "] starting task: " + name + ".");
	}
	
	@Override
	public void done() {
		System.out.println("[" + this.logPrefix + "] done.");
	}
	
	@Override
	public void subTask(String name) {
		System.out.println("[" + this.logPrefix + "] sub task: "+name+".");
	}
	
	@Override
	public boolean isCanceled() {
		return this.canceled;
	}
	
	@Override
	public void worked(int work) {
		this.workDone = this.workDone + work;
		System.out.println("[" + this.logPrefix + "] worked " + this.workDone + " of " + this.totalWork + ".");
	}
	
}