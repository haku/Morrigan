package com.vaguehope.morrigan.engines.hotkey;

import java.io.File;

public interface IHotkeyEngine {

	int MORRIGAN_HK_SHOWHIDE = 90;
	int MORRIGAN_HK_STOP = 100;
	int MORRIGAN_HK_PLAYPAUSE = 101;
	int MORRIGAN_HK_NEXT = 102;
	int MORRIGAN_HK_JUMPTO = 110;

	/**
	 * Returns the description of this engine.
	 */
	String getAbout ();

	/**
	 * This method will be called by the plugin loader shortly after
	 * the instance of the engine is created.
	 * @param classPath The array of File objects used by the
	 * classloader to load the engine.
	 */
	void setClassPath (File[] classPath);

	void registerHotkey (int id, HotkeyValue value) throws HotkeyException;

	void unregisterHotkey (int id) throws HotkeyException;

	void setListener (IHotkeyListener listener);

	/**
	 * This should be called before discarding
	 * the reference to this implementation.
	 */
	void dispose ();

}
