package com.vaguehope.morrigan.screen;

import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

public class GoFullScreenRunner implements Runnable {

	private final Monitor mon;
	private final Shell parent;
	private final ScreenMgr mgr;

	/**
	 *
	 * @param mon null to clear.
	 * @param parent may be null.
	 * @param mgr
	 */
	public GoFullScreenRunner (Monitor mon, Shell parent, ScreenMgr mgr) {
		this.mon = mon;
		this.parent = parent;
		this.mgr = mgr;
	}

	@Override
	public void run () {
		if (this.mon == null || this.mgr.isFullScreen()) {
			new RemoveFullScreenRunner(true, this.mgr).run();
		}
		else {
			startFullScreen();
		}
	}

	private void startFullScreen () {
		//Monitor refreshedMon = MonitorHelper.refreshMonitor(getSite().getShell().getDisplay(), mon);
		final ScreenMgr m = this.mgr;
		FullscreenShell fullscreenShell = new FullscreenShell(this.mgr.getDisplay(), this.parent, this.mon, new Runnable() {
			@Override
			public void run () {
				m.removeFullScreenSafe(false);
			}
		});
		this.mgr.setFullscreenShell(fullscreenShell);

		this.mgr.getRegister().registerScreenPainter(fullscreenShell.getScreenPainter());
		fullscreenShell.getShell().open();
		this.mgr.triggerUpdateCurrentMediaFrameParent();
	}

}
