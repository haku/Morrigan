package com.vaguehope.morrigan.screen;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;

import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.util.TimeHelper;

public class ScreenPainter implements PaintListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public enum ScreenType {
		TINY, MEDIUM, LARGE
	}

	public interface TitleProvider {

		PlayItem getItem ();

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	final Canvas canvas;
	private final ScreenType screenType;
	private TitleProvider titleProvider = null;
	private CoverArtProvider coverArtProvider = null;

	public ScreenPainter (final Canvas canvas, final ScreenType type) {
		this.canvas = canvas;
		this.screenType = type;

		canvas.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		canvas.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	}

	public void setTitleProvider (final TitleProvider titleProvider) {
		this.titleProvider = titleProvider;
	}

	public void setCoverArtProvider (final CoverArtProvider coverArtProvider) {
		this.coverArtProvider = coverArtProvider;
	}

	public void redrawAsync () {
		if (this.canvas.isDisposed()) return;
		this.canvas.getDisplay().asyncExec(this.redrawRunnable);
	}

	public void redrawSync () {
		if (this.canvas.isDisposed()) return;
		this.canvas.getDisplay().syncExec(this.redrawRunnable);
	}

	private final Runnable redrawRunnable = new Runnable() {
		@Override
		public void run () {
			if (ScreenPainter.this.canvas.isDisposed()) return;
			ScreenPainter.this.canvas.redraw();
		}
	};

	private final Runnable redrawSyncRunnable = new Runnable() {
		@Override
		public void run () {
			redrawSync();
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void paintControl (final PaintEvent e) {
		final Rectangle clientArea = this.canvas.getClientArea();
		final Point centre = new Point(clientArea.width / 2, clientArea.height / 2);
		final PlayItem item = this.titleProvider != null ? this.titleProvider.getItem() : null;

		if (item != null && item.hasTrack()) {
			if (this.coverArtProvider != null) {
				final Image image = this.coverArtProvider.getImage(item.getTrack(), this.redrawSyncRunnable);
				if (image != null) {
					if (!image.isDisposed()) {
						drawImageScaled(e, clientArea, image);
					}
					else {
						System.err.println("Warning: cover image for '" + item.getTrack() + "' already disposed.");
					}
				}
			}
			if (this.screenType != ScreenType.TINY) drawMainText(e, centre, item);
		}
		else {
			drawTextHVCen(e, centre.x, centre.y,
					this.screenType == ScreenType.TINY ? "[ M ]" : "[ Morrigan ]");
		}
	}

	public void drawMainText (final PaintEvent e, final Point centre, final PlayItem item) {
		final Font font = e.gc.getFont();
		Font font2;
		Font font3;

		final FontData fontData = e.gc.getFont().getFontData()[0];
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

		final String title = formatTitle(item.getTrack().getTitle());
		e.gc.setFont(font3);
		final Rectangle rect = drawTextHVCen(e, centre.x, centre.y, title);

		String counts = item.getTrack().getStartCount() + " / " + item.getTrack().getEndCount();
		if (item.getTrack().getDuration() > 0) {
			counts = counts + "     " + TimeHelper.formatTimeSeconds(item.getTrack().getDuration());
		}
		e.gc.setFont(font2);
		drawTextHCen(e, centre.x, rect.y + rect.height, counts);

		if (this.screenType != ScreenType.LARGE && item.hasList()) {
			final String listName = "(" + item.getList().getListName() + ")";
			final Point textSize = e.gc.textExtent(listName);
			drawTextHCen(e, centre.x, rect.y - textSize.y, listName);
		}

		e.gc.setFont(font);

		font2.dispose();
		font3.dispose();
	}

	private static String formatTitle (final String title) {
		return title.substring(0, title.lastIndexOf("."))
				.replace("_", " ")
				.replace(" - ", "\n")
				.replace(" ー ", "\n")
				.replace(" (", "\n(")
				.replace(" [", "\n[")
				.replace(" {", "\n{")
				.replace("【", "\n【")
				.replace("『", "\n『");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static Rectangle drawTextHCen (final PaintEvent e, final int x, final int top, final String text) {
		final Point textSize = e.gc.textExtent(text);
		final int _left = x - (textSize.x / 2);
		drawText(e, text, _left, top);
		return new Rectangle(_left, top, textSize.x, textSize.y);
	}

	private static Rectangle drawTextHVCen (final PaintEvent e, final int x, final int y, final String... text) {
		final Rectangle ret = new Rectangle(x, y, 0, 0);

		for (int i = 0; i < text.length; i++) {
			final Point textSize = e.gc.textExtent(text[i]);
			final int _left = x - (textSize.x / 2);
			final int _top = y + (textSize.y) * i - (textSize.y * text.length) / 2;

			drawText(e, text[i], _left, _top);

			if (ret.x > _left) ret.x = _left;
			if (ret.y > _top) ret.y = _top;
			if (ret.width < textSize.x) ret.width = textSize.x;
			ret.height = ret.height + textSize.y;
		}

		return ret;
	}

	private static Rectangle drawTextHVCen (final PaintEvent e, final int x, final int y, final String text) {
		final String[] split = text.split("\n");
		return drawTextHVCen(e, x, y, split);
	}

	private static void drawText (final PaintEvent e, final String text, final int x, final int y) {
		final Color savedFg = e.gc.getForeground();
		e.gc.setForeground(e.gc.getBackground());
		e.gc.drawText(text, x - 1, y - 1, SWT.DRAW_TRANSPARENT);
		e.gc.drawText(text, x + 1, y - 1, SWT.DRAW_TRANSPARENT);
		e.gc.drawText(text, x - 1, y + 1, SWT.DRAW_TRANSPARENT);
		e.gc.drawText(text, x + 1, y + 1, SWT.DRAW_TRANSPARENT);
		e.gc.setForeground(savedFg);
		e.gc.drawText(text, x, y, SWT.DRAW_TRANSPARENT);
	}

	public static void drawImageScaled (final PaintEvent e, final Rectangle clientArea, final Image image) {
		final Rectangle imgBounds = image.getBounds();

		final double s1 = clientArea.width / (double) imgBounds.width;
		final double s2 = clientArea.height / (double) imgBounds.height;

		int w, h, l, t;

		if (s1 < s2) {
			w = (int) (imgBounds.width * s1);
			h = (int) (imgBounds.height * s1);
		}
		else {
			w = (int) (imgBounds.width * s2);
			h = (int) (imgBounds.height * s2);
		}

		l = (clientArea.width / 2) - (w / 2);
		t = (clientArea.height / 2) - (h / 2);

		e.gc.drawImage(image,
				0, 0, imgBounds.width, imgBounds.height,
				l, t, w, h);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
