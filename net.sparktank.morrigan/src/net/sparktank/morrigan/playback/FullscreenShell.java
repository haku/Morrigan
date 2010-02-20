package net.sparktank.morrigan.playback;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class FullscreenShell {
	
	private Shell shell;
	private final Runnable onCloseRunnable;
	
	public FullscreenShell(Display display, Runnable onCloseRunnable) {
		shell = new Shell(display);
		this.onCloseRunnable = onCloseRunnable;
		
		shell.setLayout(new FillLayout ());
		shell.setMaximized(true);
		shell.setFullScreen(true);
		
		shell.addTraverseListener(traverseListener);
		shell.addMouseListener(mouseListener);
		shell.addShellListener(shellListener);
	}
	
	public Shell getShell () {
		return shell;
	}
	
	TraverseListener traverseListener = new TraverseListener() {
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
	
}
