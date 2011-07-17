package net.sparktank.morrigan.hotkeyimpl.jintellitype;

import net.sparktank.morrigan.engines.hotkey.HotkeyEngineFactory;
import net.sparktank.morrigan.engines.hotkey.IHotkeyEngine;

public class EngineFactory implements HotkeyEngineFactory {

	@Override
	public IHotkeyEngine getNewHotkeyEngine() {
		return new HotkeyEngine();
	}
	
}
