package com.vaguehope.morrigan.server.boot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import com.vaguehope.morrigan.screen.ScreenPainter;
import com.vaguehope.morrigan.screen.ScreenPainter.TitleProvider;
import com.vaguehope.morrigan.screen.ScreenPainterRegister;

class ScreenRegister implements ScreenPainterRegister {

	private final Display display;
	private final TitleProvider titleProvider;
	private final List<ScreenPainter> _titlePainters = Collections.synchronizedList(new ArrayList<ScreenPainter>());

	public ScreenRegister (final Display display, final TitleProvider titleProvider) {
		if (display == null) throw new IllegalArgumentException();
		if (titleProvider == null) throw new IllegalArgumentException();
		this.display = display;
		this.titleProvider = titleProvider;
	}

	public void updateTitle () {
		if (Thread.currentThread().getId() == this.display.getThread().getId()) {
			redrawAll();
		}
		else {
			this.display.asyncExec(new Runnable() {
				@Override
				public void run () {
					redrawAll();
				}
			});
		}

	}

	@Override
	public void registerScreenPainter (final ScreenPainter p) {
		if (p == null) throw new NullPointerException();

		p.setTitleProvider(this.titleProvider);
//		p.setCoverArtProvider(coverArtProvider); TODO
		this._titlePainters.add(p);
	}

	@Override
	public void unregisterScreenPainter (final ScreenPainter p) {
		this._titlePainters.remove(p);
		p.setTitleProvider(null);
	}

	protected void redrawAll () {
		for (ScreenPainter sp : this._titlePainters) {
			sp.redrawTitle();
		}
	}

}
