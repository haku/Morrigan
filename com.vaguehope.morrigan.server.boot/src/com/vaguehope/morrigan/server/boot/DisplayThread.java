package com.vaguehope.morrigan.server.boot;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;

class DisplayThread extends Thread {

	private static final Logger LOG = Logger.getLogger(DisplayThread.class.getName());

	private final boolean guiPresent;

	private volatile boolean alive = true;

	public DisplayThread (final boolean guiPresent) {
		this.guiPresent = guiPresent;
	}

	public Display getDisplay () {
		return DisplayGetter.getDefaultDisplayIfExists();
	}

	public void dispose () {
		this.alive = false;
	}

	@Override
	public void run () {
		if (this.guiPresent) {
			LOG.info("GUI bundle present, not creating UI thread.");
			return;
		}

		final Display d = makeDisplay();
		if (d == null) {
			LOG.info("UI not available.");
			return;
		}

		if (d.getThread().getId() != getId()) {
			LOG.info("UI thread already exists.");
			return;
		}

		LOG.info("Becoming psudo UI thread.");
		setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 1));
		while (!d.isDisposed() && this.alive) {
			if (!d.readAndDispatch()) d.sleep();
		}
		d.dispose();
		this.alive = false;
		LOG.info("Finished.");
	}

	/**
	 * Return null if UI is not accessible.
	 */
	private static Display makeDisplay () {
		try {
			return Display.getDefault();
		}
		catch (final SWTError e) {
			if (e.code == SWT.ERROR_NO_HANDLES) {
				return null;
			}
			throw e;
		}
	}

}
