package net.sparktank.morrigan.engines.hotkey;

import java.io.File;

public interface IHotkeyEngine {
	
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
	
	public void registerHotkey (int id, int key, boolean ctrl, boolean shift, boolean alt, boolean supr) throws HotkeyException;
	
	public void unregisterHotkey (int id) throws HotkeyException;
	
	public void setListener (IHotkeyListener listener);
	
	/**
	 * This should be called before discarding
	 * the reference to this implementation.
	 */
	public void finalise ();
	
}
