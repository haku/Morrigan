package com.vaguehope.morrigan.gui.helpers;

import java.awt.MouseInfo;
import java.awt.Point;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

public final class MonitorHelper {

	private MonitorHelper () {}

	/**
	 * Horrifying method to 'refresh' a cached monitor object. This is useful in
	 * case the display configuration has been changed, for example after
	 * screen's resolution has been changed.
	 */
	public static Monitor refreshMonitor (final Display d, final Monitor m) {
		for (int i = 0; i < d.getMonitors().length; i++) {
			Monitor mon = d.getMonitors()[i];
			if (mon.equals(m)) {
				return mon;
			}
		}
		throw new IllegalArgumentException("Monitor " + m + " does not exist.");
	}

	public static void moveShellToActiveMonitor (final Shell shell) {
		final Point mouse = MouseInfo.getPointerInfo().getLocation();
		for (final Monitor mon : shell.getDisplay().getMonitors()) {
			final Rectangle monBounds = mon.getBounds();
			if (mouse.x >= monBounds.x && mouse.x <= monBounds.x + monBounds.width
					&& mouse.y >= monBounds.y && mouse.y <= monBounds.y + monBounds.width) {

				final Rectangle shellBounds = shell.getBounds();
				final int x = monBounds.x + (monBounds.width - shellBounds.width) / 2;
				final int y = monBounds.y + (monBounds.height - shellBounds.height) / 2;
				shell.setLocation(x, y);

				break;
			}
		}
	}

}
