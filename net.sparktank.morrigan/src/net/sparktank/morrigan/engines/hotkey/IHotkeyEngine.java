package net.sparktank.morrigan.engines.hotkey;

import java.io.File;

public interface IHotkeyEngine {
	
	public static final int MORRIGAN_HK_STOP = 100;
	public static final int MORRIGAN_HK_PLAYPAUSE = 101;
	public static final int MORRIGAN_HK_NEXT = 102;
	public static final int MORRIGAN_HK_JUMPTO = 103;
	
	/**
	 * Returns the description of this engine.
	 */
	public String getAbout ();
	
	/**
	 * This method will be called by the plugin loader shortly after
	 * the instance of the engine is created.
	 * @param classPath The array of File objects used by the
	 * classloader to load the engine.
	 */
	public void setClassPath (File[] classPath);
	
	public void registerHotkey (int id, HotkeyValue value) throws HotkeyException;
	
	public void unregisterHotkey (int id) throws HotkeyException;
	
	public void setListener (IHotkeyListener listener);
	
	/**
	 * This should be called before discarding
	 * the reference to this implementation.
	 */
	public void finalise ();
	
}
