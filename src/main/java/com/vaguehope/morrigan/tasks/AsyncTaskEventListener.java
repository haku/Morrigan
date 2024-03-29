package com.vaguehope.morrigan.tasks;

import java.time.Clock;
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

	private static final String MISSING_TITLE = "(no title)";

	private static final AtomicInteger TASK_NUMBER = new AtomicInteger(0);
	private static final long EXPIRY_AGE_MILLIS = 60 * 60 * 1000L;
	private static final int ALL_MESSAGES_MAX_LENGTH = 20000;
	private static final ThreadSafeDateFormatter DATE_FORMATTER = new ThreadSafeDateFormatter("MMdd-HHmmss.SSS");
	private static final ThreadSafeDateFormatter TIME_FORMATTER = new ThreadSafeDateFormatter("HH:mm");

	private final int number;
	private final String id = UUID.randomUUID().toString();
	private final Clock clock;

	private final AtomicReference<TaskState> lifeCycle = new AtomicReference<>(TaskState.UNSTARTED);
	private final AtomicReference<TaskOutcome> outcome = new AtomicReference<>();
	private final AtomicReference<TaskOutcome> futureOutcome = new AtomicReference<>();
	private final AtomicBoolean logExceptionFromFuture = new AtomicBoolean();

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

	public AsyncTaskEventListener() {
		this(TASK_NUMBER.getAndIncrement(), Clock.systemDefaultZone());
	}

	protected AsyncTaskEventListener(final int number, final Clock clock) {
		this.number = number;
		this.clock = clock;
	}

	@Override
	public String oneLineSummary() {
		final StringBuilder s = new StringBuilder()
				.append(this.number)
				.append(" [");

		final TaskOutcome fo = getFutureOutcome();
		if (fo != null && fo != TaskOutcome.SUCCESS) {
			s.append(fo);
		}
		else if (this.lifeCycle.get() == TaskState.COMPLETE) {
			s.append(this.outcome.get());
		}
		else {
			s.append(this.lifeCycle.get());
		}

		if (this.startTime.get() > 0) {
			s.append(' ').append(TIME_FORMATTER.get().format(new Date(this.startTime.get())));
		}

		s.append(']');

		final int P = this.progressTotal.get();
		if (this.lifeCycle.get() == TaskState.RUNNING && P > 0) {
			final int p = this.progressWorked.get();
			s.append(' ').append(String.valueOf(p)).append(" of ").append(String.valueOf(P));
		}

		final String name = this.taskName.get();
		s.append(' ').append(name != null ? name : MISSING_TITLE);

		if (this.lifeCycle.get() != TaskState.COMPLETE) {
			final String subName = this.subtaskName.get();
			if (subName != null) s.append(": ").append(subName);
		}

		return s.toString();
	}

	@Override
	public String fullSummary () {
		final StringBuilder s = new StringBuilder(oneLineSummary());

		final String err = this.lastErr.get();
		if (err != null) s.append("\n    Last error: ").append(err);

		final String msg = this.lastMsg.get();
		if (msg != null) s.append("\n    Last message: ").append(msg);

		final Future<?> f = this.future.get();
		if (f != null && f.isDone()) {
			try {
				f.get(); // Check for Exception.
			}
			catch (final ExecutionException e) {
				s.append("\n    Exection of task failed: \n");
				final Throwable cause = e.getCause(); // ExecutionException should be a wrapper.
				s.append(ErrorHelper.getStackTrace(cause != null ? cause : e));
			}
			catch (final InterruptedException e) { /* Should be impossible. */ }
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
						&& this.endTime.get() + EXPIRY_AGE_MILLIS < this.clock.millis());
	}

	@Override
	public List<String> getAllMessages() {
		return Collections.unmodifiableList(this.allMessages);
	}

	private void addToAllMessages(final String msg) {
		if (this.allMessages.size() >= ALL_MESSAGES_MAX_LENGTH) return;
		boolean first = true;
		for (final String line : msg.split("\\r?\\n")) {
			if (first) {
				this.allMessages.add(DATE_FORMATTER.get().format(new Date(this.clock.millis())) + " " + line);
				first = false;
			}
			else {
				this.allMessages.add(line);
			}
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	TaskEventListener methods.

	@Override
	public void onStart () {
		if (!this.lifeCycle.compareAndSet(TaskState.UNSTARTED, TaskState.RUNNING)) {
			throw new IllegalStateException("Failed to mark task as running; current state=" + this.lifeCycle.get() + ".");
		}
		this.startTime.set(this.clock.millis());
		addToAllMessages("Started.");
	}

	@Override
	public void logMsg(String s) {
		this.lastMsg.set(s);
		addToAllMessages(s);
	}

	@Override
	public void logMsg (final String topic, final String s) {
		logMsg(topic + ": " + s);
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
	public void done(final TaskOutcome taskOutcome) {
		if (this.lifeCycle.compareAndSet(TaskState.RUNNING, TaskState.COMPLETE)) {
			this.endTime.set(this.clock.millis());
			this.outcome.compareAndSet(null, taskOutcome);
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
		final String name = this.taskName.get();
		if (StringHelper.notBlank(name)) return name;
		return MISSING_TITLE;
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
	public Boolean successful() {
		return getFutureOutcome() == TaskOutcome.SUCCESS;
	}

	private TaskOutcome getFutureOutcome() {
		final TaskOutcome cached = this.futureOutcome.get();
		if (cached != null) return cached;

		final TaskOutcome fo = resolveFutureOutcome();
		if (fo == null) return null;

		if (this.futureOutcome.compareAndSet(null, fo)) {
			return fo;
		}
		return this.futureOutcome.get();
	}

	private TaskOutcome resolveFutureOutcome() {
		final Future<?> f = this.future.get();
		if (f == null) return null;
		if (!f.isDone()) return null;
		if (f.isCancelled()) return TaskOutcome.CANCELLED;

		try {
			f.get(); // Check for Exception.
			return TaskOutcome.SUCCESS;
		}
		catch (final ExecutionException e) {
			if (this.logExceptionFromFuture.compareAndSet(false, true)) {
				Throwable ex = e;
				if (e.getCause() != null) ex = e.getCause();
				logError("Failed", "", ex);
			}
			return TaskOutcome.FAILED;
		}
		catch (final InterruptedException e) {
			throw new IllegalStateException("Should not be possible to interupt non blocking call.");
		}
	}

}
