package com.vaguehope.morrigan.server.boot;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;

class DisplayThread extends Thread {

	private static final Logger LOG = Logger.getLogger(DisplayThread.class.getName());

	private final AtomicReference<Display> displayCache = new AtomicReference<Display>();

	public Display getDisplay () {
		return this.displayCache.get();
	}

	public void dispose () {
		this.displayCache.set(null);
	}

	@Override
	public void run () {
		// FIXME do not create a Display if RCP is present but not as quick to start up as we are.
		try { // FIXME this is a nasty hack around.
			Thread.sleep(TimeUnit.SECONDS.toMillis(15));
		}
		catch (InterruptedException e) {/* Do not care. */}

		Display d = makeDisplay();
		if (d != null) {
			this.displayCache.set(d);
			if (d.getThread().getId() == getId()) {
				LOG.info("Using psudo UI thread.");
				setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 1));
				while (!d.isDisposed() && this.displayCache.get() != null) {
					if (!d.readAndDispatch()) d.sleep();
				}
				d.dispose();
				this.displayCache.set(null);
			}
			else {
				LOG.info("Using RCP UI thread.");
			}
		}
		else {
			LOG.info("UI not available.");
		}
	}

	/**
	 * Return null if UI is not accessible.
	 */
	private static Display makeDisplay () {
		try {
			return Display.getDefault();
		}
		catch (SWTError e) {
			if (e.code == SWT.ERROR_NO_HANDLES) {
				return null;
			}
			throw e;
		}
	}

}
