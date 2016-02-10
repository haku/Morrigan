package com.vaguehope.morrigan.tasks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
	private final AtomicInteger lifeCycle = new AtomicInteger(0);

	private final AtomicInteger progressWorked = new AtomicInteger(0);
	private final AtomicInteger progressTotal = new AtomicInteger(0);
	private final AtomicBoolean cancelled = new AtomicBoolean(false);

	private final AtomicReference<String> taskName = new AtomicReference<String>(null);
	private final AtomicReference<String> subtaskName = new AtomicReference<String>(null);

	private final AtomicReference<String> lastMsg = new AtomicReference<String>(null);
	private final AtomicReference<String> lastErr = new AtomicReference<String>(null);

	private final AtomicLong endTime = new AtomicLong();

	private final AtomicReference<Future<?>> future = new AtomicReference<Future<?>>();

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

		Future<?> f = this.future.get();
		if (f != null && f.isDone()) {
			try {
				f.get(); // Check for Exception.
			}
			catch (ExecutionException e) {
				s.append("\n    Exection of task failed: \n");
				Throwable cause = e.getCause(); // ExecutionException should be a wrapper.
				s.append(ErrorHelper.getStackTrace(cause != null ? cause : e));
			}
			catch (InterruptedException e) { /* Should be impossible. */ }
		}

		return s.toString();
	}

	public void setFuture (final Future<?> future) {
		if (!this.future.compareAndSet(null, future)) {
			throw new IllegalStateException("Future has already been set.");
		}
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
		if (!this.lifeCycle.compareAndSet(0, 1)) {
			throw new IllegalStateException("Failed to mark task as running; current state=" + this.lifeCycle.get() + ".");
		}
	}

	@Override
	public void logMsg (final String topic, final String s) {
		this.lastMsg.set(topic + ": " + s);
	}

	@Override
	public void logError (final String topic, final String s, final Throwable t) {
		this.lastErr.set(topic + ": " + s + "\n" + ErrorHelper.getCauseTrace(t));
	}

	@Override
	public void beginTask (final String name, final int totalWork) {
		this.taskName.set(name);
		this.progressTotal.set(totalWork);
	}

	@Override
	public void subTask (final String name) {
		this.subtaskName.set(name);
	}

	@Override
	public void done () {
		if (this.lifeCycle.compareAndSet(1, 2)) {
			this.endTime.set(System.currentTimeMillis());
		}
	}

	@Override
	public boolean isCanceled () {
		return this.cancelled.get();
	}

	@Override
	public void worked (final int work) {
		this.progressWorked.addAndGet(work);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
