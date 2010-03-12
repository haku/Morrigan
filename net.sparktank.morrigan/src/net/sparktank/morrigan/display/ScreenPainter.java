package net.sparktank.morrigan.display;


import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.media.PlayItem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

public class ScreenPainter implements PaintListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public enum ScreenType {TINY, MEDIUM, LARGE};
	
	public interface TitleProvider {
		
		abstract public PlayItem getItem ();
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Canvas canvas;
	private final ScreenType type;
	private TitleProvider titleProvider;
	
	public ScreenPainter(Canvas canvas, ScreenType type) {
		this.canvas = canvas;
		this.type = type;
		
		canvas.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		canvas.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	}
	
	public void setTitleProvider (TitleProvider titleProvider) {
		this.titleProvider = titleProvider;
	}
	
	public void redrawTitle () {
		canvas.getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (!canvas.isDisposed()) {
					canvas.redraw();
				}
			}
		});
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void paintControl(PaintEvent e) {
		Rectangle clientArea = canvas.getClientArea();
		
		if (type != ScreenType.TINY) {
			PlayItem item = titleProvider.getItem();
			Point centre = new Point(clientArea.width/2, clientArea.height/2);
			
			String text;
			
			if (item != null && item.item != null) {
				text = item.item.getTitle();
				text = text.substring(0, text.lastIndexOf("."));
				text = text.replace(" - ", "\n");
				
				text = text + "\n\n" + item.item.getStartCount() + " / " + item.item.getEndCount();
				if (item.item.getDuration() > 0) {
					text = text + "   " + TimeHelper.formatTime(item.item.getDuration());
				}
				
				if (item.list != null) {
					text = "(" + item.list.getListName() + ")\n\n" + text;
				}
				
			} else {
				text = "[ Morrigan ]";
			}
			
			drawTextCen(e, centre.x, centre.y, text);
			
		}
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
	
	private void drawTextCen (PaintEvent e, int x, int y, String text) {
		String[] split = text.split("\n");
		drawTextCen(e, x, y, split);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
