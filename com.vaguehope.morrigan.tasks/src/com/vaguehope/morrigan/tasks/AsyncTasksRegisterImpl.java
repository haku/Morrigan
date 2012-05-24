package com.vaguehope.morrigan.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class AsyncTasksRegisterImpl implements AsyncTasksRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Set<AsyncTaskEventListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<AsyncTaskEventListener, Boolean>());
	private final ExecutorService executor;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public AsyncTasksRegisterImpl (ExecutorService executor) {
		this.executor = executor;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void scheduleTask (final MorriganTask task) {
		final AsyncTaskEventListener taskEventListener = makeTrackedListener();
		Runnable runnable = new Runnable() {
			@Override
			public void run () {
				task.run(taskEventListener);
			}
		};
		Future<?> future = this.executor.submit(runnable);
		taskEventListener.setFuture(future);
	}

	private AsyncTaskEventListener makeTrackedListener () {
		AsyncTaskEventListener l = new AsyncTaskEventListener();
		trackListener(l);
		return l;
	}

	private void trackListener (AsyncTaskEventListener listener) {
		clean();
		this.listeners.add(listener);
	}

	@Override
	public String reportSummary () {
		clean();
		StringBuilder s = new StringBuilder();
		if (this.listeners.size() > 0) {
			for (AsyncTaskEventListener l : this.listeners) {
				s.append(l.summarise()).append('\n');
			}
		}
		else {
			s.append("No tasks to report.\n");
		}
		return s.toString();
	}

	@Override
	public String[] reportIndiviually () {
		clean();
		int n = this.listeners.size();
		List<String> ret = new ArrayList<String>(n);
		for (AsyncTaskEventListener l : this.listeners) {
			ret.add(l.summarise());
		}
		return ret.toArray(new String[n]);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void clean () {
		List<AsyncTaskEventListener> toRemove = null;
		for (AsyncTaskEventListener l : this.listeners) {
			if (l.isExpired()) {
				if (toRemove == null) toRemove = new LinkedList<AsyncTaskEventListener>();
				toRemove.add(l);
			}
		}
		if (toRemove != null) {
			for (AsyncTaskEventListener l : toRemove) {
				this.listeners.remove(l);
			}
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
