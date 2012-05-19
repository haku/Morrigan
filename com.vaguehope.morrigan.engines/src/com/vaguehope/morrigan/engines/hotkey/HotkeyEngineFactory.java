package com.vaguehope.morrigan.engines.hotkey;

public interface HotkeyEngineFactory {

	IHotkeyEngine newHotkeyEngine ();

	boolean canMakeHotkeyEngine();

}
