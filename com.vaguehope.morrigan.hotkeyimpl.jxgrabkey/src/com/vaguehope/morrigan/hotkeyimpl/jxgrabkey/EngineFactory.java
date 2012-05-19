package com.vaguehope.morrigan.hotkeyimpl.jxgrabkey;

import com.vaguehope.morrigan.engines.hotkey.HotkeyEngineFactory;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyEngine;

public class EngineFactory implements HotkeyEngineFactory {

	@Override
	public IHotkeyEngine newHotkeyEngine () {
		return new HotkeyEngine();
	}

	@Override
	public boolean canMakeHotkeyEngine () {
		return true;
	}

}
