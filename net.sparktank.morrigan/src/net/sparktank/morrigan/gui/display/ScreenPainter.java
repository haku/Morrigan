package net.sparktank.morrigan.gui.display;


import net.sparktank.morrigan.helpers.TimeHelper;
import net.sparktank.morrigan.model.playlist.PlayItem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
	private final ScreenType screenType;
	private TitleProvider titleProvider = null;
	
	public ScreenPainter(Canvas canvas, ScreenType type) {
		this.canvas = canvas;
		this.screenType = type;
		
		canvas.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		canvas.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	}
	
	public void setTitleProvider (TitleProvider titleProvider) {
		this.titleProvider = titleProvider;
	}
	
	public void redrawTitle () {
		if (!canvas.isDisposed()) canvas.getShell().getDisplay().asyncExec(new Runnable() {
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
		
		if (screenType != ScreenType.TINY) {
			PlayItem item = titleProvider.getItem();
			Point centre = new Point(clientArea.width/2, clientArea.height/2);
			
			String text;
			
			if (item != null && item.item != null) {
				Font font = e.gc.getFont();
				Font font2;
				Font font3;
				
				FontData fontData = e.gc.getFont().getFontData()[0];
				if (screenType == ScreenType.LARGE) {
					font2 = new Font(e.gc.getDevice(), fontData.getName(),
							fontData.getHeight() * 2, fontData.getStyle());
					font3 = new Font(e.gc.getDevice(), fontData.getName(),
							fontData.getHeight() * 3, fontData.getStyle());
					
				} else {
					font2 = new Font(e.gc.getDevice(), fontData);
					font3 = new Font(e.gc.getDevice(), fontData.getName(),
							(int)(fontData.getHeight() * 1.5f), fontData.getStyle());
				}
				
				text = item.item.getTitle();
				text = text.substring(0, text.lastIndexOf("."));
				text = text.replace(" - ", "\n");
				e.gc.setFont(font3);
				Rectangle rect = drawTextHVCen(e, centre.x, centre.y, text);
				
				text = item.item.getStartCount() + " / " + item.item.getEndCount();
				if (item.item.getDuration() > 0) {
					text = text + "   " + TimeHelper.formatTimeSeconds(item.item.getDuration());
				}
				e.gc.setFont(font2);
				Rectangle rect2 = drawTextHCen(e, centre.x, rect.y + rect.height, text);
				
				if (item.list != null) {
					text = "(" + item.list.getListName() + ")";
					Point textSize = e.gc.textExtent(text);
					drawTextHCen(e, centre.x, rect.y - textSize.y, text);
				}
				
				e.gc.setFont(font);
				
				if (screenType == ScreenType.LARGE) {
					text = item.item.getFilepath();
					drawTextHCen(e, centre.x, rect2.y + rect2.height, text);
				}
				
				font2.dispose();
				font3.dispose();
				
			} else {
				text = "[ Morrigan ]";
				drawTextHVCen(e, centre.x, centre.y, text);
			}
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Rectangle drawTextHCen (PaintEvent e, int x, int top, String text) {
		Point textSize = e.gc.textExtent(text);
		int _left = x - (textSize.x / 2);
		e.gc.drawText(text, _left, top, SWT.TRANSPARENT);
		return new Rectangle(_left, top, textSize.x, textSize.y);
	}
	
	private Rectangle drawTextHVCen (PaintEvent e, int x, int y, String... text) {
		Rectangle ret = new Rectangle(x, y, 0, 0);
		
		for (int i=0; i < text.length; i++) {
			Point textSize = e.gc.textExtent(text[i]);
			int _left = x - (textSize.x / 2);
			int _top = y + (textSize.y) * i - (textSize.y * text.length) / 2;
			
			e.gc.drawText(text[i], _left, _top, SWT.TRANSPARENT);
			
			if (ret.x > _left) ret.x = _left;
			if (ret.y > _top) ret.y = _top;
			if (ret.width < textSize.x) ret.width = textSize.x;
			ret.height = ret.height + textSize.y;
		}
		
		return ret;
	}
	
	private Rectangle drawTextHVCen (PaintEvent e, int x, int y, String text) {
		String[] split = text.split("\n");
		return drawTextHVCen(e, x, y, split);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
