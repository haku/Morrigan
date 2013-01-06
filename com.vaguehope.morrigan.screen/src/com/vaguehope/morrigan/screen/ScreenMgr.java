package com.vaguehope.morrigan.screen;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

public class ScreenMgr {

	private final Display display;
	private final ScreenPainterRegister register;
	private final ScreenMgrCallback callback;

	private FullscreenShell fullscreenShell = null;

	public ScreenMgr (Display display, ScreenPainterRegister register, ScreenMgrCallback callback) {
		this.register = register;
		if (display == null) throw new IllegalArgumentException("Display is required.");
		if (register == null) throw new IllegalArgumentException("Register is required.");
		if (callback == null) throw new IllegalArgumentException("Callback is required.");
		this.display = display;
		this.callback = callback;
	}

	public boolean isFullScreen () {
		return this.fullscreenShell != null;
	}

	/**
	 * Null monitor clears full screen.
	 */
	public void goFullScreenSafe (Monitor mon) {
		if (mon != null) {
			runOnUiThread(new GoFullScreenRunner(mon, null, this));
		}
		else {
			removeFullScreenSafe(true);
		}
	}

	public void removeFullScreenSafe (boolean closeShell) {
		runOnUiThread(new RemoveFullScreenRunner(closeShell, this));
	}

	public Composite getCurrentVideoParent () {
		Composite fsParent = getFullScreenVideoParent();
		if (fsParent != null) return fsParent;
		return this.callback.getCurrentScreen();
	}

	public Composite getFullScreenVideoParent () {
		if (!isFullScreen()) return null;
		return this.fullscreenShell.getShell();
	}

	void handleError (Exception e) {
		this.callback.handleError(e);
	}

	void triggerUpdateCurrentMediaFrameParent () {
		this.callback.updateCurrentMediaFrameParent(getFullScreenVideoParent());
	}

	Display getDisplay () {
		return this.display;
	}

	ScreenPainterRegister getRegister () {
		return this.register;
	}

	ScreenMgrCallback getCallback () {
		return this.callback;
	}

	void setFullscreenShell (FullscreenShell fullscreenShell) {
		this.fullscreenShell = fullscreenShell;
	}

	FullscreenShell getFullscreenShell () {
		return this.fullscreenShell;
	}

	private void runOnUiThread (Runnable r) {
		if (Thread.currentThread().getId() == this.display.getThread().getId()) {
			r.run();
		}
		else {
			this.display.asyncExec(r);
		}
	}

}
