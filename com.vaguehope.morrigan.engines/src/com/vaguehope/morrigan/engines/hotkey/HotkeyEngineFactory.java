package com.vaguehope.morrigan.engines.hotkey;

public interface HotkeyEngineFactory {

	// TODO remote 'get' from method name.
	IHotkeyEngine getNewHotkeyEngine ();

	boolean canMakeHotkeyEngine();

}
