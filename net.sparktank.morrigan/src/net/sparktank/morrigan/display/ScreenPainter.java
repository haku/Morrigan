package net.sparktank.morrigan.display;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

public class ScreenPainter implements PaintListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Canvas canvas;
	
	public ScreenPainter(Canvas canvas) {
		this.canvas = canvas;
		
		canvas.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		canvas.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void paintControl(PaintEvent e) {
		Rectangle clientArea = canvas.getClientArea();
		
		Point centre = new Point(clientArea.width/2, clientArea.height/2);
		drawTextCen(e, centre.x, centre.y, "Morrigan");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void drawTextCen (PaintEvent e, int x, int y, String... text) {
		for (int i=0; i<text.length; i++) {
			Point textSize = e.gc.textExtent(text[i]);
			e.gc.drawText(text[i],
					x-(textSize.x/2),
					y + (textSize.y) * i - (textSize.y * text.length)/2, 
					SWT.TRANSPARENT);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
