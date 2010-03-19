package net.sparktank.morrigan.engines.hotkey;

public interface IHotkeyListener {
	
	public enum CanDo {YES, NO, MAYBE, YESANDFRIENDS};
	
	public CanDo canDoKeyPress (int id);
	
	public void onKeyPress (int id);
	
}
