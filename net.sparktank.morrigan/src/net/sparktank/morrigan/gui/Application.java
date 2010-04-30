package net.sparktank.morrigan.gui;

import java.io.IOException;
import java.net.URL;

import net.sparktank.morrigan.config.Config;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * This class controls all aspects of the application's execution.
 */
public class Application implements IApplication {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) {
		Display display = PlatformUI.createDisplay();
		try {
			// Set workspace location.
			Location instanceLoc = Platform.getInstanceLocation();
			if (!instanceLoc.isSet()) {
				String configDir = Config.getConfigDir();
				try {
					instanceLoc.set(new URL("file", null, configDir), false);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			System.out.println("instanceLoc=" + instanceLoc.getURL().toExternalForm());
			
			// Run workbench.
			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART) {
				return IApplication.EXIT_RESTART;
			}
			return IApplication.EXIT_OK;
			
		} finally {
			display.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null) return;
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				if (!display.isDisposed()) workbench.close();
			}
		});
	}
}
