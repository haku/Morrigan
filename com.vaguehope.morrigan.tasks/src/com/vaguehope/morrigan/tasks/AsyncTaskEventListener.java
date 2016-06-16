package com.vaguehope.morrigan.tasks;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.vaguehope.morrigan.util.ErrorHelper;
import com.vaguehope.morrigan.util.StringHelper;

public class AsyncTaskEventListener implements TaskEventListener, AsyncTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final long EXPIRY_AGE = 30 * 60 * 1000L; // 30 minutes.

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final String id = UUID.randomUUID().toString();

	private final AtomicReference<TaskState> lifeCycle = new AtomicReference<TaskState>(TaskState.UNSTARTED);

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

		s.append('[').append(this.lifeCycle.get()).append(']');

		int P = this.progressTotal.get();
		if (this.lifeCycle.get() == TaskState.RUNNING && P > 0) {
			int p = this.progressWorked.get();
			s.append(' ').append(String.valueOf(p)).append(" of ").append(String.valueOf(P));
		}

		String name = this.taskName.get();
		s.append(' ').append(name != null ? name : "<task>");

		if (this.lifeCycle.get() != TaskState.COMPLETE) {
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
		return this.lifeCycle.get() == TaskState.COMPLETE
				&& (this.endTime.get() > 0
						&& this.endTime.get() + EXPIRY_AGE < System.currentTimeMillis());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	TaskEventListener methods.

	@Override
	public void onStart () {
		if (!this.lifeCycle.compareAndSet(TaskState.UNSTARTED, TaskState.RUNNING)) {
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
		if (this.lifeCycle.compareAndSet(TaskState.RUNNING, TaskState.COMPLETE)) {
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

	@Override
	public String id () {
		return this.id;
	}
	@Override
	public String title () {
		String name = this.taskName.get();
		if (StringHelper.notBlank(name)) return name;
		return "<task>";
	}
	@Override
	public TaskState state () {
		return this.lifeCycle.get();
	}
	@Override
	public String subtask () {
		final String s = this.subtaskName.get();
		return s != null ? s : "";
	}
	@Override
	public String lastMsg () {
		final String s = this.lastMsg.get();
		return s != null ? s : "";
	}
	@Override
	public String lastErr () {
		final String s = this.lastErr.get();
		return s != null ? s : "";
	}
	@Override
	public int progressWorked () {
		return this.progressWorked.get();
	}
	@Override
	public int progressTotal () {
		return this.progressTotal.get();
	}
	@Override
	public Boolean successful () {
		final Future<?> f = this.future.get();
		if (f == null || !f.isDone()) return null;

		try {
			f.get(); // Check for Exception.
			return true;
		}
		catch (ExecutionException e) {
			return false;
		}
		catch (InterruptedException e) {
			throw new IllegalStateException("Should not be possible to interupt non blocking call.");
		}
	}
	@Override
	public String summary () {
		return summarise();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
