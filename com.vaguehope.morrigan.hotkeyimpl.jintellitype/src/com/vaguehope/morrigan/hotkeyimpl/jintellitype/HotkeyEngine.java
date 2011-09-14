package com.vaguehope.morrigan.hotkeyimpl.jintellitype;

import java.io.File;


import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.IntellitypeListener;
import com.melloware.jintellitype.JIntellitype;
import com.melloware.jintellitype.JIntellitypeConstants;
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
	public void setClassPath(File[] classPath) {
		// Unused.
	}
	
	@Override
	public void registerHotkey(int id, HotkeyValue value) throws HotkeyException {
		if (!JIntellitype.isJIntellitypeSupported()) {
			throw new HotkeyException("JIntellitype is not available.");
		}
		
		setup();
		
		int mask = 0;
		if (value.getCtrl()) mask += JIntellitypeConstants.MOD_CONTROL;
		if (value.getShift()) mask += JIntellitypeConstants.MOD_SHIFT;
		if (value.getAlt()) mask += JIntellitypeConstants.MOD_ALT;
		if (value.getSupr()) mask += JIntellitypeConstants.MOD_WIN;
		
		try {
			JIntellitype.getInstance().registerHotKey(id, mask, value.getKey());
		} catch (Throwable t) {
			throw new HotkeyException("Failed to register hotkey " + mask + "+" + value.getKey(), t);
		}
	}
	
	@Override
	public void unregisterHotkey(int id) throws HotkeyException {
		JIntellitype.getInstance().unregisterHotKey(id);
	}
	
	@Override
	public void setListener(IHotkeyListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void finalise() {
		teardown();
		JIntellitype.getInstance().cleanUp();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	boolean haveSetup = false;
	
	private void setup () {
		if (this.haveSetup) return;
		JIntellitype.getInstance().addHotKeyListener(this.hotkeyListener);
		JIntellitype.getInstance().addIntellitypeListener(this.intellitypeListener);
		this.haveSetup = true;
	}
	
	private void teardown () {
		if (!this.haveSetup) return;
		JIntellitype.getInstance().removeHotKeyListener(this.hotkeyListener);
		JIntellitype.getInstance().removeIntellitypeListener(this.intellitypeListener);
		this.haveSetup = false;
	}
	
	private HotkeyListener hotkeyListener = new HotkeyListener() {
		@Override
		public void onHotKey(int identifier) {
			callListener(identifier);
		}
	};
	
	private IntellitypeListener intellitypeListener = new IntellitypeListener() {
		@Override
		public void onIntellitype(int aCommand) {
			switch (aCommand) {
				
				case JIntellitypeConstants.APPCOMMAND_MEDIA_STOP:
					callListener(MORRIGAN_HK_STOP);
					break;
					
				case JIntellitypeConstants.APPCOMMAND_MEDIA_PLAY_PAUSE:
					callListener(MORRIGAN_HK_PLAYPAUSE);
					break;
					
				case JIntellitypeConstants.APPCOMMAND_MEDIA_NEXTTRACK:
					callListener(MORRIGAN_HK_NEXT);
					break;
				
			}
		}
	}; 
	
	protected void callListener(int id) {
		if (this.listener!=null) {
			this.listener.onKeyPress(id);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
