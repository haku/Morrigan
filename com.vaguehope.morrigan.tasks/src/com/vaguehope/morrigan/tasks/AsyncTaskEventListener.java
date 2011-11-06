package com.vaguehope.morrigan.tasks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.vaguehope.morrigan.util.ErrorHelper;

public class AsyncTaskEventListener implements TaskEventListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final long EXPIRY_AGE = 30 * 60 * 1000L; // 30 minutes.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * 0 = unstarted.
	 * 1 = started.
	 * 2 = complete.
	 */
	private AtomicInteger lifeCycle = new AtomicInteger(0);
	
	private AtomicInteger progressWorked = new AtomicInteger(0);
	private AtomicInteger progressTotal = new AtomicInteger(0);
	private AtomicBoolean cancelled = new AtomicBoolean(false);
	
	private AtomicReference<String> taskName = new AtomicReference<String>(null);
	private AtomicReference<String> subtaskName = new AtomicReference<String>(null);
	
	private AtomicReference<String> lastMsg = new AtomicReference<String>(null);
	private AtomicReference<String> lastErr = new AtomicReference<String>(null);
	
	private AtomicLong endTime = new AtomicLong();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public String summarise () {
		StringBuilder s = new StringBuilder();
		
		String state;
		switch (this.lifeCycle.get()) {
			case 0: state = "Unstarted"; break;
			case 1: state = "Running"; break;
			case 2: state = "Complete"; break;
			default: throw new IllegalStateException();
		}
		s.append('[').append(state).append(']');
		
		int P = this.progressTotal.get();
		if (this.lifeCycle.get() == 1 && P > 0) {
			int p = this.progressWorked.get();
			s.append(' ').append(String.valueOf(p)).append(" of ").append(String.valueOf(P));
		}
		
		String name = this.taskName.get();
		s.append(' ').append(name != null ? name : "<task>");
		
		if (this.lifeCycle.get() < 2) {
			String subName = this.subtaskName.get();
			if (subName != null) s.append(": ").append(subName);
		}
		
		String err = this.lastErr.get();
		if (err != null) s.append("\n    Last error: ").append(err);
		
		String msg = this.lastMsg.get();
		if (msg != null) s.append("\n    Last message: ").append(msg);
		
		return s.toString();
	}
	
	public void cancel () {
		this.cancelled.set(true);
	}
	
	public boolean isExpired () {
		return this.lifeCycle.get() == 2
				&& (this.endTime.get() > 0
						&& this.endTime.get() + EXPIRY_AGE < System.currentTimeMillis());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	TaskEventListener methods.
	
	@Override
	public void onStart () {
		this.lifeCycle.compareAndSet(0, 1);
	}

	@Override
	public void logMsg (String topic, String s) {
		this.lastMsg.set(topic + ": " + s);
	}

	@Override
	public void logError (String topic, String s, Throwable t) {
		this.lastErr.set(topic + ": " + s + "\n" + ErrorHelper.getCauseTrace(t));
	}

	@Override
	public void beginTask (String name, int totalWork) {
		this.taskName.set(name);
		this.progressTotal.set(totalWork);
	}

	@Override
	public void subTask (String name) {
		this.subtaskName.set(name);
	}

	@Override
	public void done () {
		this.lifeCycle.compareAndSet(1, 2);
		this.endTime.set(System.currentTimeMillis());
	}

	@Override
	public boolean isCanceled () {
		return this.cancelled.get();
	}

	@Override
	public void worked (int work) {
		this.progressWorked.addAndGet(work);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
