package com.vaguehope.morrigan.server.boot;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class NullScreen {

	private final UiMgr uiMgr;
	private volatile Shell shell;

	public NullScreen (UiMgr uiMgr) {
		this.uiMgr = uiMgr;
	}

	public Composite getScreen () {
		if (this.shell != null) return this.shell;
		if (this.uiMgr == null) return null;
		final Display display = this.uiMgr.getDisplay();
		if (display == null) return null;
		display.syncExec(new Runnable() {
			@Override
			public void run () {
				setShell(new Shell(display));
			}
		});
		return this.shell;
	}

	public void dispose () {
		if (this.shell != null) {
			final Shell s = this.shell;
			this.shell.getDisplay().syncExec(new Runnable() {
				@Override
				public void run () {
					s.dispose();
				}
			});
		}
	}

	protected void setShell (Shell shell) {
		this.shell = shell;
	}

}
