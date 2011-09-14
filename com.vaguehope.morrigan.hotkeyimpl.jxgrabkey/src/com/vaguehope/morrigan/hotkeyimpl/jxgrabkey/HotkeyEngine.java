package com.vaguehope.morrigan.hotkeyimpl.jxgrabkey;

import java.awt.event.InputEvent;
import java.io.File;

import com.vaguehope.morrigan.engines.hotkey.HotkeyException;
import com.vaguehope.morrigan.engines.hotkey.HotkeyValue;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyEngine;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyListener;

import jxgrabkey.HotkeyListener;
import jxgrabkey.JXGrabKey;


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
	public void setClassPath(File[] classPath) { /* UNUSED */ }
	
	@Override
	public void registerHotkey(int id, HotkeyValue value) throws HotkeyException {
		loadSo();
		setup();
		
		int mask = 0;
		if (value.getCtrl()) mask += InputEvent.CTRL_MASK;
		if (value.getShift()) mask += InputEvent.SHIFT_MASK;
		if (value.getAlt()) mask += InputEvent.ALT_MASK;
		if (value.getSupr()) mask += InputEvent.META_MASK;
		
		try {
			JXGrabKey.getInstance().registerAwtHotkey(id, mask, value.getKey());
		} catch (Throwable t) {
			throw new HotkeyException("Failed to register hotkey " + mask + "+" + value.getKey(), t);
		}
	}
	
	@Override
	public void unregisterHotkey(int id) throws HotkeyException {
		if (!this.soLoaded || !this.haveSetup) return;
		JXGrabKey.getInstance().unregisterHotKey(id);
	}
	
	@Override
	public void setListener(IHotkeyListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void finalise() {
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
	
	private HotkeyListener hotkeyListener = new jxgrabkey.HotkeyListener(){
		@Override
		public void onHotkey(int hotkey_idx) {
			callListener(hotkey_idx);
		}
	};
	
	protected void callListener(int id) {
		if (this.listener!=null) {
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
