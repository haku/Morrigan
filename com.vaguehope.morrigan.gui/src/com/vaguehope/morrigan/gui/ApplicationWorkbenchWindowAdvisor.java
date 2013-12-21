package com.vaguehope.morrigan.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import com.vaguehope.morrigan.engines.hotkey.IHotkeyEngine;
import com.vaguehope.morrigan.gui.helpers.TrayHelper;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	public ApplicationWorkbenchWindowAdvisor(final IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(final IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(700, 500));
		configurer.setShowMenuBar(false);
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(true);
		configurer.setShowProgressIndicator(true);
	}

	@Override
	public void postWindowOpen() {
		super.postWindowOpen();

		getWindowConfigurer().getWindow().getShell().addListener(SWT.Iconify, new Listener() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void handleEvent(final Event event) {
				TrayHelper.minToTray(getWindowConfigurer().getWindow(), false);
			}
		});

		getWindowConfigurer().getWindow().getShell().getDisplay().addFilter(SWT.KeyDown, new SwtHotKeyListener());
	}

	/**
	 * FIXME move this somewhere better.
	 */
	private static class SwtHotKeyListener implements Listener {

		public SwtHotKeyListener () {}

		@Override
		public void handleEvent (final Event e) {
			int action = 0;
			if ((e.stateMask & SWT.CTRL) == SWT.CTRL) {
				if ((e.stateMask & SWT.SHIFT) == SWT.SHIFT) {
					if (e.keyCode == ' ') {
						action = IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE;
					}
					else if (e.keyCode == SWT.ARROW_RIGHT) {
						action = IHotkeyEngine.MORRIGAN_HK_NEXT;
					}
				}
				else if ((e.stateMask & SWT.ALT) == SWT.ALT && (e.keyCode == 'x')) {
					action = IHotkeyEngine.MORRIGAN_HK_JUMPTO;
				}
			}
			if (action > 0) {
				Activator.getHotkeyRegister().sendHotkeyEvent(action);
				e.type = SWT.None;
			}
		}

	}

}
