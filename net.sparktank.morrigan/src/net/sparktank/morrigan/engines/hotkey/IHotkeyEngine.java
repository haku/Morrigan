package net.sparktank.morrigan.engines.hotkey;

public interface IHotkeyEngine {
	
	public void registerHotkey (int id, int key, boolean ctrl, boolean shift, boolean alt, boolean supr) throws HotkeyException;
	
	public void unregisterHotkey (int id) throws HotkeyException;
	
	public void setListener (IHotkeyListener listener);
	
}
