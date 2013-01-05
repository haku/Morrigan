package com.vaguehope.morrigan.server.boot;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;

class DisplayThread extends Thread {

	private final AtomicReference<Display> displayCache = new AtomicReference<Display>();

	public Display getDisplay () {
		return this.displayCache.get();
	}

	public void dispose () {
		this.displayCache.set(null);
	}

	@Override
	public void run () {
		Display d = makeDisplay();
		if (d == null) return;
		this.displayCache.set(d);

		if (d.getThread().getId() != getId()) return;

		setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 1));
		while (!d.isDisposed() && this.displayCache.get() != null) {
			if (!d.readAndDispatch()) d.sleep();
		}
		d.dispose();
		this.displayCache.set(null);
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
