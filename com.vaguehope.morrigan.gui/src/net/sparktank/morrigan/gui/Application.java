package net.sparktank.morrigan.gui;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;


import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vaguehope.morrigan.config.Config;

/**
 * This class controls all aspects of the application's execution.
 */
public class Application implements IApplication {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public Object start (IApplicationContext context) {
		Display display = PlatformUI.createDisplay();
		try {
			// Set workspace location.
			Location instanceLoc = Platform.getInstanceLocation();
			if (!instanceLoc.isSet()) {
				String configDir = Config.getConfigDir();
				try {
					instanceLoc.set(new URL("file", null, configDir), false);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			this.logger.info("instanceLoc=" + instanceLoc.getURL().toExternalForm());
			
			// Run workbench.
			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART) {
				return IApplication.EXIT_RESTART;
			}
			return IApplication.EXIT_OK;
		}
		finally {
			display.dispose();
		}
	}
	
	@Override
	public void stop () {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null) return;
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				if (!display.isDisposed()) workbench.close();
			}
		});
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
