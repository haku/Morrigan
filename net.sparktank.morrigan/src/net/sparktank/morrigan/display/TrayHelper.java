package net.sparktank.morrigan.display;

import net.sparktank.morrigan.Activator;
import net.sparktank.morrigan.preferences.GeneralPref;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IWorkbenchWindow;

public class TrayHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static boolean minToTray (final IWorkbenchWindow window, boolean force) {
		if (!GeneralPref.getMinToTray() && !force) {
			return false;
		}
		
		Tray systemTray = window.getShell().getDisplay().getSystemTray();
		if (systemTray==null) {
			return false;
		}
		
		final TrayItem trayItem = new TrayItem(systemTray, SWT.NONE);
		trayItem.setToolTipText(window.getShell().getText());
		final Image image = Activator.getImageDescriptor("icons/copipod.ico").createImage();
		trayItem.setImage(image);
		
		window.getShell().setVisible(false);
		
		final Menu menu = new Menu(window.getShell(), SWT.POP_UP);
		final MenuItem open = new MenuItem(menu, SWT.PUSH);
		open.setText("&Open");
		final MenuItem exit = new MenuItem(menu, SWT.PUSH);
		exit.setText("E&xit");
		
		Listener showEventListener = new Listener() {
			public void handleEvent(Event event) {
				Shell workbenchWindowShell = window.getShell();
				workbenchWindowShell.setMinimized(false);
				workbenchWindowShell.setVisible(true);
				workbenchWindowShell.setActive();
				workbenchWindowShell.setFocus();
				trayItem.dispose();
				image.dispose();
				open.dispose();
				exit.dispose();
				menu.dispose();
			}
		};
		
		Listener exitEventListener = new Listener() {
			public void handleEvent(Event event) {
				trayItem.dispose();
				image.dispose();
				open.dispose();
				exit.dispose();
				menu.dispose();
				window.getWorkbench().close();
			}
		};
		
		open.addListener(SWT.Selection, showEventListener);
		exit.addListener(SWT.Selection, exitEventListener);
		
		trayItem.addListener(SWT.MenuDetect, new Listener() {
			public void handleEvent(Event event) {
				menu.setVisible(true);
			}
		});
		trayItem.addListener(SWT.Selection, showEventListener);
		
		return true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
