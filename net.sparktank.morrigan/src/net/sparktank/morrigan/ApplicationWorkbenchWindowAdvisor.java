package net.sparktank.morrigan;

import net.sparktank.morrigan.display.TrayHelper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(700, 500));
		configurer.setShowMenuBar(false);
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(false);
		configurer.setShowProgressIndicator(true);
	}
	
	@Override
	public void postWindowOpen() {
		super.postWindowOpen();
		
		getWindowConfigurer().getWindow().getShell().addListener(SWT.Iconify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				TrayHelper.minToTray(getWindowConfigurer().getWindow(), false);
			}
		});
	}
	
	@Override
	public boolean preWindowShellClose() {
		return !TrayHelper.minToTray(getWindowConfigurer().getWindow(), false);
	}
	
}
