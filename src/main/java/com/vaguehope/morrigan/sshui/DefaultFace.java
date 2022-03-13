package com.vaguehope.morrigan.sshui;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.input.KeyStroke;

public abstract class DefaultFace implements Face {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultFace.class);

	private final FaceNavigation navigation;
	private final BlockingQueue<Callable<Void>> onUiThread = new LinkedBlockingQueue<>();

	public DefaultFace (final FaceNavigation navigation) {
		this.navigation = navigation;
	}

	@Override
	public void onFocus() throws Exception {
		// Do nothing by default.
	}

	@Override
	public boolean onInput (final KeyStroke k, final WindowBasedTextGUI gui) throws Exception {
		switch (k.getKeyType()) {
			case Escape:
				return this.navigation.backOneLevel();
			case Character:
				switch (k.getCharacter()) {
					case 'q':
						return this.navigation.backOneLevel();
					default:
				}
			//$FALL-THROUGH$
		default:
				return false;
		}
	}

	@Override
	public void onClose () throws Exception {
		// Do nothing by default.
	}

	public void scheduleOnUiThread (final Callable<Void> callable) {
		this.onUiThread.add(callable);
	}

	@Override
	public boolean processEvents () {
		boolean ret = false;
		Callable<Void> c;
		while ((c = this.onUiThread.poll()) != null) {
			try {
				c.call();
			}
			catch (final Exception e) {
				LOG.warn("Background task failed.", e);
			}
			ret = true;
		}
		return ret;
	}

}
