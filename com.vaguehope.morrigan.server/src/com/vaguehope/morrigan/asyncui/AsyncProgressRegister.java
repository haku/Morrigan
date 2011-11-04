package com.vaguehope.morrigan.asyncui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.vaguehope.morrigan.model.tasks.TaskEventListener;

public class AsyncProgressRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	
	private AsyncProgressRegister () {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	
	// TODO FIXME stop this being static.  It does not play nice with bundle updating.
	// There is no concurrent set.
	static private final ConcurrentHashMap<AsyncTaskEventListener, Boolean> listeners = new ConcurrentHashMap<AsyncTaskEventListener, Boolean>();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	
	static public TaskEventListener makeTrackedListener () {
		AsyncTaskEventListener l = new AsyncTaskEventListener();
		trackListener(l);
		return l;
	}
	
	static public void trackListener (AsyncTaskEventListener listener) {
		clean();
		listeners.put(listener, Boolean.TRUE); // Place-holder value.
	}
	
	static public String reportSummary () {
		clean();
		StringBuilder s = new StringBuilder();
		Set<AsyncTaskEventListener> ls = listeners.keySet();
		if (ls.size() > 0) {
			for (AsyncTaskEventListener l : ls) {
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
		Set<AsyncTaskEventListener> ls = listeners.keySet();
		int n = ls.size();
		List<String> ret = new ArrayList<String>(n);
		for (AsyncTaskEventListener l : ls) {
			ret.add(l.summarise());
		}
		return ret.toArray(new String[n]);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	
	static private void clean () {
		List<AsyncTaskEventListener> toRemove = null;
		Set<AsyncTaskEventListener> ls = listeners.keySet();
		for (AsyncTaskEventListener l : ls) {
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
