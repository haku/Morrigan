package com.vaguehope.morrigan.engines.hotkey;

public interface IHotkeyListener {

	enum CanDo {YES, NO, MAYBE, YESANDFRIENDS}

	CanDo canDoKeyPress (int id);

	void onKeyPress (int id);

}
