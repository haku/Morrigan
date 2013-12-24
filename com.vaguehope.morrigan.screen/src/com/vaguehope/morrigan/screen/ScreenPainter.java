package com.vaguehope.morrigan.screen;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.util.TimeHelper;

public class ScreenPainter implements PaintListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public enum ScreenType {TINY, MEDIUM, LARGE}

	public interface TitleProvider {

		PlayItem getItem ();

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	final Canvas canvas;
	private final ScreenType screenType;
	private TitleProvider titleProvider = null;

	public ScreenPainter (final Canvas canvas, final ScreenType type) {
		this.canvas = canvas;
		this.screenType = type;

		canvas.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		canvas.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	}

	public void setTitleProvider (final TitleProvider titleProvider) {
		this.titleProvider = titleProvider;
	}

	public void redrawTitle () {
		if (!this.canvas.isDisposed()) this.canvas.getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run () {
				if (!ScreenPainter.this.canvas.isDisposed()) {
					ScreenPainter.this.canvas.redraw();
				}
			}
		});
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void paintControl (final PaintEvent e) {
		final Rectangle clientArea = this.canvas.getClientArea();
		final Point centre = new Point(clientArea.width / 2, clientArea.height / 2);
		final PlayItem item = this.titleProvider != null ? this.titleProvider.getItem() : null;

		if (this.screenType == ScreenType.TINY) {
			drawTextHVCen(e, centre.x, centre.y, "[ M ]");
		}
		else if (item != null && item.item != null) {
			Font font = e.gc.getFont();
			Font font2;
			Font font3;

			FontData fontData = e.gc.getFont().getFontData()[0];
			if (this.screenType == ScreenType.LARGE) {
				font2 = new Font(e.gc.getDevice(), fontData.getName(),
						fontData.getHeight() * 2, fontData.getStyle());
				font3 = new Font(e.gc.getDevice(), fontData.getName(),
						fontData.getHeight() * 3, fontData.getStyle());

			}
			else {
				font2 = new Font(e.gc.getDevice(), fontData);
				font3 = new Font(e.gc.getDevice(), fontData.getName(),
						(int) (fontData.getHeight() * 1.5f), fontData.getStyle());
			}

			String title = item.item.getTitle();
			title = title.substring(0, title.lastIndexOf("."));
			title = title.replace(" - ", "\n");
			e.gc.setFont(font3);
			Rectangle rect = drawTextHVCen(e, centre.x, centre.y, title);

			String counts = item.item.getStartCount() + " / " + item.item.getEndCount();
			if (item.item.getDuration() > 0) {
				counts = counts + "   " + TimeHelper.formatTimeSeconds(item.item.getDuration());
			}
			e.gc.setFont(font2);
			Rectangle rect2 = drawTextHCen(e, centre.x, rect.y + rect.height, counts);

			if (item.list != null) {
				String listName = "(" + item.list.getListName() + ")";
				Point textSize = e.gc.textExtent(listName);
				drawTextHCen(e, centre.x, rect.y - textSize.y, listName);
			}

			e.gc.setFont(font);

			if (this.screenType == ScreenType.LARGE) {
				drawTextHCen(e, centre.x, rect2.y + rect2.height, item.item.getFilepath());
			}

			font2.dispose();
			font3.dispose();
		}
		else {
			drawTextHVCen(e, centre.x, centre.y, "[ Morrigan ]");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static Rectangle drawTextHCen (final PaintEvent e, final int x, final int top, final String text) {
		Point textSize = e.gc.textExtent(text);
		int _left = x - (textSize.x / 2);
		e.gc.drawText(text, _left, top, SWT.TRANSPARENT);
		return new Rectangle(_left, top, textSize.x, textSize.y);
	}

	private static Rectangle drawTextHVCen (final PaintEvent e, final int x, final int y, final String... text) {
		Rectangle ret = new Rectangle(x, y, 0, 0);

		for (int i = 0; i < text.length; i++) {
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

	private static Rectangle drawTextHVCen (final PaintEvent e, final int x, final int y, final String text) {
		String[] split = text.split("\n");
		return drawTextHVCen(e, x, y, split);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
