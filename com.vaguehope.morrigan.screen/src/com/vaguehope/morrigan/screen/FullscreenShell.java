package com.vaguehope.morrigan.screen;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.vaguehope.morrigan.screen.ScreenPainter.ScreenType;

public class FullscreenShell {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	final Shell shell;
	private final ScreenPainter screenPainter;
	final Runnable onCloseRunnable;
	
	public FullscreenShell(Shell parent, Monitor mon, Runnable onCloseRunnable) {
		this.shell = new Shell(parent.getDisplay(), SWT.ON_TOP);
		this.onCloseRunnable = onCloseRunnable;
		
		this.shell.setText("Morrigan Screen");
		this.shell.setImage(parent.getImage());
		this.shell.setLayout(new FillLayout());
		
		Point pt = new Point(mon.getBounds().x, mon.getBounds().y);
		this.shell.setLocation(pt);
		this.shell.setSize(mon.getBounds().width, mon.getBounds().height);
		this.shell.setMaximized(true);
		this.shell.setFullScreen(true);
		
		this.screenPainter = new ScreenPainter(this.shell, ScreenType.LARGE);
		this.shell.addPaintListener(this.screenPainter);
		
		this.shell.addTraverseListener(this.traverseListener);
		this.shell.addMouseListener(this.mouseListener);
		this.shell.addShellListener(this.shellListener);
	}
	
	public Shell getShell () {
		return this.shell;
	}
	
	public ScreenPainter getScreenPainter() {
		return this.screenPainter;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private TraverseListener traverseListener = new TraverseListener() {
		@Override
		public void keyTraversed(TraverseEvent e) {
			if (e.detail==SWT.TRAVERSE_ESCAPE) {
				e.detail = SWT.TRAVERSE_NONE;
				e.doit = false;
				FullscreenShell.this.shell.close();
			}
		}
	};
	
	private final MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			FullscreenShell.this.shell.close();
		}
		
		@Override
		public void mouseUp(MouseEvent e) {/* UNUSED */}
		@Override
		public void mouseDown(MouseEvent e) {/* UNUSED */}
		
	};
	
	private final ShellListener shellListener = new ShellListener() {
		@Override
		public void shellClosed(ShellEvent e) {
			FullscreenShell.this.onCloseRunnable.run();
		}
		
		@Override
		public void shellActivated(ShellEvent e) {/* UNUSED */}
		@Override
		public void shellDeactivated(ShellEvent e) {/* UNUSED */}
		@Override
		public void shellIconified(ShellEvent e) {/* UNUSED */}
		@Override
		public void shellDeiconified(ShellEvent e) {/* UNUSED */}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

}
