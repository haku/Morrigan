package com.vaguehope.morrigan.gui.util;

import java.awt.MouseInfo;
import java.awt.Point;

import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

public final class MonitorHelper {

	private MonitorHelper () {
		throw new AssertionError();
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
