package com.vaguehope.morrigan.hotkeyimpl.jxgrabkey;

import java.awt.event.InputEvent;
import java.io.File;

import jxgrabkey.HotkeyListener;
import jxgrabkey.JXGrabKey;

import com.vaguehope.morrigan.engines.hotkey.HotkeyException;
import com.vaguehope.morrigan.engines.hotkey.HotkeyValue;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyEngine;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyListener;

public class HotkeyEngine implements IHotkeyEngine {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private IHotkeyListener listener;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructor.

	public HotkeyEngine () {/* UNUSED */}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	IHotkeyEngine methods.

	@Override
	public String getAbout () {
		return "com.vaguehope.morrigan.hotkeyimpl.jintellitype version 0.01.";
	}

	@Override
	public void setClassPath (final File[] classPath) { /* UNUSED */}

	@Override
	public void registerHotkey (final int id, final HotkeyValue value) throws HotkeyException {
		loadSo();
		setup();

		int mask = 0;
		if (value.getCtrl()) mask += InputEvent.CTRL_MASK;
		if (value.getShift()) mask += InputEvent.SHIFT_MASK;
		if (value.getAlt()) mask += InputEvent.ALT_MASK;
		if (value.getSupr()) mask += InputEvent.META_MASK;

		try {
			JXGrabKey.getInstance().registerAwtHotkey(id, mask, value.getKey());
		}
		catch (Exception e) { // NOSONAR library likely to throw unexpected things.
			throw new HotkeyException("Failed to register hotkey " + mask + "+" + value.getKey(), e);
		}
	}

	@Override
	public void unregisterHotkey (final int id) throws HotkeyException {
		if (!this.soLoaded || !this.haveSetup) return;
		JXGrabKey.getInstance().unregisterHotKey(id);
	}

	@Override
	public void setListener (final IHotkeyListener listener) {
		this.listener = listener;
	}

	@Override
	public void dispose () {
		teardown();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	boolean haveSetup = false;

	private void setup () {
		if (this.haveSetup) return;

		JXGrabKey.setDebugOutput(false);
		JXGrabKey.getInstance().addHotkeyListener(this.hotkeyListener);

		this.haveSetup = true;
	}

	private void teardown () {
		if (!this.haveSetup) return;

		JXGrabKey.getInstance().removeHotkeyListener(this.hotkeyListener);
		JXGrabKey.getInstance().cleanUp();

		this.haveSetup = false;
	}

	private final HotkeyListener hotkeyListener = new jxgrabkey.HotkeyListener() {
		@Override
		public void onHotkey (final int hotkey_idx) {
			callListener(hotkey_idx);
		}
	};

	protected void callListener (final int id) {
		if (this.listener != null) {
			this.listener.onKeyPress(id);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String SONAME = "JXGrabKey";
	private boolean soLoaded = false;

	private void loadSo () {
		if (this.soLoaded) return;
		System.loadLibrary(SONAME);
		this.soLoaded = true;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
