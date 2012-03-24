package com.vaguehope.morrigan.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class AsyncProgressRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private AsyncProgressRegister () {}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	// TODO FIXME stop this being static.  It does not play nice with bundle updating.
	// There is no concurrent set.
	static private final Collection<AsyncTaskEventListener> listeners = new ConcurrentLinkedQueue<AsyncTaskEventListener>();

	static private final ExecutorService executor = Executors.newCachedThreadPool();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void scheduleTask (final IMorriganTask task) {
		final AsyncTaskEventListener taskEventListener = makeTrackedListener();
		final Runnable runnable = new Runnable() {
			@Override
			public void run () {
				task.run(taskEventListener);
			}
		};
		Future<?> future = executor.submit(runnable);
		taskEventListener.setFuture(future);
	}

	private static AsyncTaskEventListener makeTrackedListener () {
		AsyncTaskEventListener l = new AsyncTaskEventListener();
		trackListener(l);
		return l;
	}

	static private void trackListener (AsyncTaskEventListener listener) {
		clean();
		listeners.add(listener);
	}

	static public String reportSummary () {
		clean();
		StringBuilder s = new StringBuilder();
		if (listeners.size() > 0) {
			for (AsyncTaskEventListener l : listeners) {
				s.append(l.summarise()).append('\n');
			}
		}
		else {
			s.append("No tasks to report.\n");
		}
		return s.toString();
	}

	static public String[] reportIndiviually () {
		clean();
		int n = listeners.size();
		List<String> ret = new ArrayList<String>(n);
		for (AsyncTaskEventListener l : listeners) {
			ret.add(l.summarise());
		}
		return ret.toArray(new String[n]);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	static private void clean () {
		List<AsyncTaskEventListener> toRemove = null;
		for (AsyncTaskEventListener l : listeners) {
			if (l.isExpired()) {
				if (toRemove == null) toRemove = new LinkedList<AsyncTaskEventListener>();
				toRemove.add(l);
			}
		}
		if (toRemove != null) {
    		for (AsyncTaskEventListener l : toRemove) {
    			listeners.remove(l);
    		}
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
