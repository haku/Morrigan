package com.vaguehope.morrigan.gui.helpers;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

public class MonitorHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Horrifying method to 'refresh' a cached monitor object.
	 * This is useful in case the display configuration has been changed,
	 * for example after screen's resolution has been changed.
	 */
	public static Monitor refreshMonitor (Display d, Monitor m) {
		for (int i = 0; i < d.getMonitors().length; i++) {
			Monitor mon = d.getMonitors()[i];
			if (mon.equals(m)) {
				return mon;
			}
		}
		throw new IllegalArgumentException("Monitor " + m + " does not exist.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
