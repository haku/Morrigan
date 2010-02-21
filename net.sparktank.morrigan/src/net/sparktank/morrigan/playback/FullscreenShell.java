package net.sparktank.morrigan.playback;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

public class FullscreenShell {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final Shell shell;
	private final Runnable onCloseRunnable;
	
	public FullscreenShell(Display display, Monitor mon, Runnable onCloseRunnable) {
		shell = new Shell(display);
		this.onCloseRunnable = onCloseRunnable;
		
		shell.setLayout(new FillLayout());
		
		Point pt = new Point(mon.getClientArea().x + 1, mon.getClientArea().y + 1);
		shell.setLocation(pt);
		shell.setMaximized(true);
		shell.setFullScreen(true);
		
		shell.addPaintListener(new ScreenPaintListener(shell));
		
		shell.addTraverseListener(traverseListener);
		shell.addMouseListener(mouseListener);
		shell.addShellListener(shellListener);
	}
	
	public Shell getShell () {
		return shell;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private class ScreenPaintListener implements PaintListener {
		
		private final Canvas canvas;
		
		public ScreenPaintListener(Canvas canvas) {
			this.canvas = canvas;
		}
		
		@Override
		public void paintControl(PaintEvent e) {
			Rectangle clientArea = canvas.getClientArea();
			
			Point centre = new Point(clientArea.width/2, clientArea.height/2);
			drawTextCen(e, centre.x, centre.y, "Morrigan");
		}
		
		private void drawTextCen (PaintEvent e, int x, int y, String... text) {
			for (int i=0; i<text.length; i++) {
				Point textSize = e.gc.textExtent(text[i]);
				e.gc.drawText(text[i],
						x-(textSize.x/2),
						y + (textSize.y) * i - (textSize.y * text.length)/2, 
						SWT.TRANSPARENT);
			}
		}
		
	};
	
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
