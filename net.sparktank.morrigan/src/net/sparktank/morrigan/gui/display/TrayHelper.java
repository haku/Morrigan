package net.sparktank.morrigan.gui.display;

import net.sparktank.morrigan.gui.Activator;
import net.sparktank.morrigan.gui.preferences.GeneralPref;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IWorkbenchWindow;

public class TrayHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * This method must be called on the UI thread.
	 */
	static public boolean minToTray (final IWorkbenchWindow window, boolean force) {
		if (!GeneralPref.getMinToTray() && !force) {
			return false;
		}
		
		Tray systemTray = window.getShell().getDisplay().getSystemTray();
		if (systemTray==null) {
			return false;
		}
		
		final TrayItem trayItem = new TrayItem(systemTray, SWT.NONE);
		trayItem.setToolTipText(window.getShell().getText());
		final Image image = Activator.getImageDescriptor("icons/crow-16.png").createImage();
		trayItem.setImage(image);
		
		window.getShell().setVisible(false);
		
		final Menu menu = new Menu(window.getShell(), SWT.POP_UP);
		final MenuItem open = new MenuItem(menu, SWT.PUSH);
		open.setText("&Open");
		final MenuItem exit = new MenuItem(menu, SWT.PUSH);
		exit.setText("E&xit");
		
		final Runnable cleanup = new Runnable() {
			@Override
			public void run() {
				trayItem.dispose();
				image.dispose();
				open.dispose();
				exit.dispose();
				menu.dispose();
			}
		};
		
		Listener showEventListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				window.getShell().setMinimized(false);
				window.getShell().open();
			}
		};
		
		Listener exitEventListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				window.getWorkbench().close();
			}
		};
		
		open.addListener(SWT.Selection, showEventListener);
		exit.addListener(SWT.Selection, exitEventListener);
		
		trayItem.addListener(SWT.MenuDetect, new Listener() {
			@Override
			public void handleEvent(Event event) {
				menu.setVisible(true);
			}
		});
		trayItem.addListener(SWT.Selection, showEventListener);
		
		window.getShell().addShellListener(new ShellListener() {
			@Override
			public void shellActivated(ShellEvent e) {
				window.getShell().removeShellListener(this);
				cleanup.run();
			}
			@Override
			public void shellDeiconified(ShellEvent e) {
				window.getShell().removeShellListener(this);
				cleanup.run();
			}
			@Override
			public void shellClosed(ShellEvent e) {
				window.getShell().removeShellListener(this);
				cleanup.run();
			}
			@Override
			public void shellDeactivated(ShellEvent e) {/* UNUSED */}
			@Override
			public void shellIconified(ShellEvent e) {/* UNUSED */}
		});
		
		return true;
	}
	
	/**
	 * This method must be called on the UI thread.
	 */
	static public void hideShowWindow (final IWorkbenchWindow window) {
//		if (window.getShell().getDisplay().getActiveShell() != null) { // This does not work with multiple windows.
		if (window.getShell().isVisible()) {
			minToTray(window, true);
		} else {
			window.getShell().setMinimized(false);
			window.getShell().open();
			window.getShell().setFocus();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
