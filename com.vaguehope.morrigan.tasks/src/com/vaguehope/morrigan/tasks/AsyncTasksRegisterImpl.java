package com.vaguehope.morrigan.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class AsyncTasksRegisterImpl implements AsyncTasksRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Set<AsyncTaskEventListener> listeners = new CopyOnWriteArraySet<AsyncTaskEventListener>();
	private final ExecutorService executor;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public AsyncTasksRegisterImpl (final ExecutorService executor) {
		this.executor = executor;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void scheduleTask (final MorriganTask task) {
		if (task == null) throw new IllegalArgumentException();
		final AsyncTaskEventListener taskEventListener = makeTrackedListener();
		final Runnable runnable = new Runnable() {
			@Override
			public void run () {
				final TaskResult result = task.run(taskEventListener);
				switch (result.getOutcome()) {
					case CANCELED:
						taskEventListener.logMsg("Result", "cancelled.");
						break;
					case FAILED:
						taskEventListener.logError("Failed", result.getErrMsg(), result.getErrThr());
						break;
					case SUCCESS:
					default:
				}
				taskEventListener.done();
			}
		};
		final Future<?> future = this.executor.submit(runnable);
		taskEventListener.setFuture(future);
	}

	private AsyncTaskEventListener makeTrackedListener () {
		clean();
		final AsyncTaskEventListener l = new AsyncTaskEventListener();
		this.listeners.add(l);
		return l;
	}

	@Override
	public String reportSummary () {
		clean();
		if (this.listeners.size() < 1) return "No tasks to report.\n";

		final StringBuilder s = new StringBuilder();
		for (final AsyncTaskEventListener l : this.listeners) {
			s.append(l.summarise()).append('\n');
		}
		return s.toString();
	}

	@Override
	public String[] reportIndiviually () {
		clean();
		final List<String> ret = new ArrayList<String>();
		for (final AsyncTaskEventListener l : this.listeners) {
			ret.add(l.summarise());
		}
		return ret.toArray(new String[ret.size()]);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void clean () {
		for (AsyncTaskEventListener l : this.listeners) {
			if (l.isExpired()) this.listeners.remove(l);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
