package com.vaguehope.morrigan.screen;


public class RemoveFullScreenRunner implements Runnable {

	private final boolean closeShell;
	private final ScreenMgr mgr;

	/**
	 *
	 * @param closeShell this will be false if we are responding to the user having already closed the window.
	 */
	public RemoveFullScreenRunner (boolean closeShell, ScreenMgr mgr) {
		this.closeShell = closeShell;
		this.mgr = mgr;
	}

	@Override
	public void run () {
		if (!this.mgr.isFullScreen()) return;

		try {
			this.mgr.getRegister().unregisterScreenPainter(this.mgr.getFullscreenShell().getScreenPainter());

			if (this.closeShell) this.mgr.getFullscreenShell().getShell().close();

			FullscreenShell fs = this.mgr.getFullscreenShell();
			this.mgr.setFullscreenShell(null);
			this.mgr.triggerUpdateCurrentMediaFrameParent();

			if (fs != null && !fs.getShell().isDisposed()) {
				fs.getShell().dispose();
			}

		}
		catch (Exception e) {
			this.mgr.handleError(e);
		}
	}

}
