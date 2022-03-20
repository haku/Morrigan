package com.vaguehope.morrigan.tasks;

public class MultitaskEventListener implements TaskEventListener {

	private final TaskEventListener parent;

	public MultitaskEventListener (final TaskEventListener parent) {
		this.parent = parent;
	}

	@Override
	public void onStart () {
		this.parent.onStart();
	}

	@Override
	public void logMsg (final String topic, final String s) {
		this.parent.logMsg(topic, s);
	}

	@Override
	public void logError (final String topic, final String s, final Throwable t) {
		this.parent.logError(topic, s, t);
	}

	@Override
	public void setName (final String name) {
		this.parent.setName(name);
	}

	public void beginTask (final String name) {
		this.parent.beginTask(name, 100);
	}

	@Override
	public void beginTask (final String name, final int totalWork) {
		throw new UnsupportedOperationException("Setting totalWork not supported.");
	}

	@Override
	public void subTask (final String name) {
		this.parent.subTask(name);
	}

	@Override
	public void done(final TaskOutcome outcome) {
		this.parent.done(outcome);
	}

	@Override
	public boolean isCanceled () {
		return this.parent.isCanceled();
	}

	@Override
	public void worked (final int work) {
		throw new UnsupportedOperationException("Not supported - work via subtasks.");
	}

	public TaskEventListener newSubTaskListener (final int percentage) {
		return new SubTaskListener(this.parent, percentage);
	}

	private static class SubTaskListener implements TaskEventListener {

		private final TaskEventListener parent;
		private final int percentage;
		private int totalWork;

		public SubTaskListener (final TaskEventListener parent, final int percentage) {
			this.parent = parent;
			this.percentage = percentage;
		}

		@Override
		public void onStart () {
			// NOOP.
		}

		@Override
		public void logMsg (final String topic, final String s) {
			this.parent.logMsg(topic, s);
		}

		@Override
		public void logError (final String topic, final String s, final Throwable t) {
			this.parent.logError(topic, s, t);
		}

		@Override
		public void setName (final String name) {
			this.parent.subTask(name);
		}

		@Override
		public void beginTask (final String name, final int totalWork) {
			this.parent.subTask(name);
			this.totalWork = totalWork;
		}

		@Override
		public void subTask (final String name) {
			this.parent.subTask(name);
		}

		@Override
		public void done(final TaskOutcome outcome) {
			// NOOP.
		}

		@Override
		public boolean isCanceled () {
			return this.parent.isCanceled();
		}

		private volatile int progressWorked = 0;
		private volatile int progressReported = 0;

		@Override
		public synchronized void worked (final int work) {
			this.progressWorked += work;

			final int p = this.progressWorked * this.percentage / this.totalWork;
			if (p > this.progressReported) {
				this.parent.worked(p - this.progressReported);
				this.progressReported = p;
			}
		}

	}

}
