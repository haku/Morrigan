package com.vaguehope.morrigan.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.vaguehope.morrigan.util.ErrorHelper;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.util.ThreadSafeDateFormatter;

public class AsyncTaskEventListener implements TaskEventListener, AsyncTask {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final AtomicInteger TASK_NUMBER = new AtomicInteger(0);
	private static final long EXPIRY_AGE = 30 * 60 * 1000L; // 30 minutes.
	private static final int ALL_MESSAGES_MAX_LENGTH = 10000;
	private static final ThreadSafeDateFormatter DATE_FORMATTER = new ThreadSafeDateFormatter("yyyyMMdd-HHmm");
	private static final ThreadSafeDateFormatter TIME_FORMATTER = new ThreadSafeDateFormatter("HH:mm");

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final int number = TASK_NUMBER.getAndIncrement();
	private final String id = UUID.randomUUID().toString();

	private final AtomicReference<TaskState> lifeCycle = new AtomicReference<>(TaskState.UNSTARTED);

	private final AtomicInteger progressWorked = new AtomicInteger(0);
	private final AtomicInteger progressTotal = new AtomicInteger(0);
	private final AtomicBoolean cancelled = new AtomicBoolean(false);

	private final AtomicReference<String> taskName = new AtomicReference<>(null);
	private final AtomicReference<String> subtaskName = new AtomicReference<>(null);

	private final AtomicReference<String> lastMsg = new AtomicReference<>(null);
	private final AtomicReference<String> lastErr = new AtomicReference<>(null);

	private final List<String> allMessages = Collections.synchronizedList(new ArrayList<>());

	private final AtomicLong startTime = new AtomicLong();
	private final AtomicLong endTime = new AtomicLong();

	private final AtomicReference<Future<?>> future = new AtomicReference<>();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public String summarise () {
		final StringBuilder s = new StringBuilder()
				.append(this.number)
				.append(" [")
				.append(this.lifeCycle.get()).append(' ')
				.append(TIME_FORMATTER.get().format(new Date(this.startTime.get())))
				.append(']');

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

	@Override
	public void cancel () {
		this.cancelled.set(true);
		addToAllMessages("Cancelled.");
	}

	public boolean isExpired () {
		return this.lifeCycle.get() == TaskState.COMPLETE
				&& (this.endTime.get() > 0
						&& this.endTime.get() + EXPIRY_AGE < System.currentTimeMillis());
	}

	@Override
	public List<String> getAllMessages() {
		return Collections.unmodifiableList(this.allMessages);
	}

	private void addToAllMessages(String msg) {
		if (this.allMessages.size() >= ALL_MESSAGES_MAX_LENGTH) return;
		this.allMessages.add(DATE_FORMATTER.get().format(new Date()) + " " + msg);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	TaskEventListener methods.

	@Override
	public void onStart () {
		if (!this.lifeCycle.compareAndSet(TaskState.UNSTARTED, TaskState.RUNNING)) {
			throw new IllegalStateException("Failed to mark task as running; current state=" + this.lifeCycle.get() + ".");
		}
		this.startTime.set(System.currentTimeMillis());
		addToAllMessages("Started.");
	}

	@Override
	public void logMsg (final String topic, final String s) {
		String m = topic + ": " + s;
		this.lastMsg.set(m);
		addToAllMessages(m);
	}

	@Override
	public void logError (final String topic, final String s, final Throwable t) {
		String err = topic + ": " + s;
		if (t != null) {
			err += "\n";
			if (t instanceof RuntimeException) {
				err += ErrorHelper.getStackTrace(t);
			}
			else {
				err += ErrorHelper.getCauseTrace(t);
			}
		}
		this.lastErr.set(err);
		addToAllMessages(err);
	}

	@Override
	public void setName (final String name) {
		this.taskName.set(name);
	}

	@Override
	public void beginTask (final String name, final int totalWork) {
		this.taskName.set(name);
		this.progressTotal.set(totalWork);
		addToAllMessages("Begin Task: " + name);
	}

	@Override
	public void subTask (final String name) {
		this.subtaskName.set(name);
		addToAllMessages("Subtask: " + name);
	}

	@Override
	public void done () {
		if (this.lifeCycle.compareAndSet(TaskState.RUNNING, TaskState.COMPLETE)) {
			this.endTime.set(System.currentTimeMillis());
			addToAllMessages("Done.");
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
	public int number () {
		return this.number;
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
