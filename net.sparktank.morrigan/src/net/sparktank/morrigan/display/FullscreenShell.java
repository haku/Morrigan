package net.sparktank.morrigan.display;

import net.sparktank.morrigan.display.ScreenPainter.ScreenType;

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

public class FullscreenShell {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Shell shell;
	private final ScreenPainter screenPainter;
	private final Runnable onCloseRunnable;
	
	public FullscreenShell(Shell parent, Monitor mon, Runnable onCloseRunnable) {
		shell = new Shell(parent.getDisplay(), SWT.ON_TOP);
		this.onCloseRunnable = onCloseRunnable;
		
		shell.setText("Morrigan Screen");
		shell.setImage(parent.getImage());
		shell.setLayout(new FillLayout());
		
		Point pt = new Point(mon.getClientArea().x + 1, mon.getClientArea().y + 1);
		shell.setLocation(pt);
		shell.setMaximized(true);
		shell.setFullScreen(true);
		
		screenPainter = new ScreenPainter(shell, ScreenType.LARGE);
		shell.addPaintListener(screenPainter);
		
		shell.addTraverseListener(traverseListener);
		shell.addMouseListener(mouseListener);
		shell.addShellListener(shellListener);
	}
	
	public Shell getShell () {
		return shell;
	}
	
	public ScreenPainter getScreenPainter() {
		return screenPainter;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private TraverseListener traverseListener = new TraverseListener() {
		@Override
		public void keyTraversed(TraverseEvent e) {
			if (e.detail==SWT.TRAVERSE_ESCAPE) {
				shell.close();
			}
		}
	};
	
	private final MouseListener mouseListener = new MouseListener() {
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			shell.close();
		}
		
		@Override
		public void mouseUp(MouseEvent e) {}
		@Override
		public void mouseDown(MouseEvent e) {}
		
	};
	
	private final ShellListener shellListener = new ShellListener() {
		@Override
		public void shellClosed(ShellEvent e) {
			onCloseRunnable.run();
		}
		
		@Override
		public void shellActivated(ShellEvent e) {}
		@Override
		public void shellDeactivated(ShellEvent e) {}
		@Override
		public void shellIconified(ShellEvent e) { }
		@Override
		public void shellDeiconified(ShellEvent e) {}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

}
