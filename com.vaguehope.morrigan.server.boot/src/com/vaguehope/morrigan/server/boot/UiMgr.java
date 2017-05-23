package com.vaguehope.morrigan.server.boot;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

public class UiMgr {

	private final DisplayThread displayThread;

	public UiMgr (final boolean guiPresent) {
		this.displayThread = new DisplayThread(guiPresent);
		this.displayThread.start();
	}

	public void dispose () {
		this.displayThread.dispose();
	}

	public Display getDisplay () {
		return this.displayThread.getDisplay();
	}

	public Map<Integer, String> getMonitorNames () {
		Map<Integer, Monitor> monitors = getMonitors();
		if (monitors == null) return null;
		Map<Integer, String> ret = new HashMap<Integer, String>();
		for (Entry<Integer, Monitor> e : monitors.entrySet()) {
			Rectangle bounds = e.getValue().getBounds();
			ret.put(e.getKey(), bounds.width + "x" + bounds.height);
		}
		return ret;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final long MONITOR_CACHE_MAX_AGE_MILLIS = 1000; // 1 second.

	private volatile long monitorCacheAge = 0;
	private volatile Map<Integer, Monitor> monitorsCache = null;

	public Map<Integer, Monitor> getMonitors () {
		if (isMonitorCacheExpired()) {
			updateMonitorCache();
		}
		return getMonitorsCache();
	}

	public Monitor getMonitor (final int monitorIndex) {
		Map<Integer, Monitor> monitors = getMonitors();
		if (monitors == null) return null;
		return monitors.get(Integer.valueOf(monitorIndex));
	}

	protected void setMonitorsCache (final Map<Integer, Monitor> mons) {
		this.monitorsCache = (mons == null ? null : Collections.unmodifiableMap(new LinkedHashMap<Integer, Monitor>(mons)));
		this.monitorCacheAge = System.currentTimeMillis();
	}

	private Map<Integer, Monitor> getMonitorsCache () {
		return this.monitorsCache;
	}

	private boolean isMonitorCacheExpired () {
		return this.monitorsCache == null || this.monitorCacheAge <= 0 || System.currentTimeMillis() - this.monitorCacheAge > MONITOR_CACHE_MAX_AGE_MILLIS;
	}

	private void updateMonitorCache () {
		final Display display = this.displayThread.getDisplay();
		if (display == null) {
			setMonitorsCache(null);
			return;
		}
		display.syncExec(new Runnable() {
			@Override
			public void run () {
				Map<Integer, Monitor> ret = new LinkedHashMap<Integer, Monitor>();
				Monitor[] monitors = display.getMonitors();
				for (int i = 0; i < monitors.length; i++) {
					ret.put(Integer.valueOf(i), monitors[i]);
				}
				setMonitorsCache(ret);
			}
		});
	}

}
