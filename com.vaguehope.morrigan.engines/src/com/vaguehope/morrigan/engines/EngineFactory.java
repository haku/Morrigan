package com.vaguehope.morrigan.engines;

import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.hotkey.HotkeyEngineRegister;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyEngine;

public class EngineFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final Logger logger = Logger.getLogger(EngineFactory.class.getName());

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static boolean canMakeHotkeyEngine () {
		return (HotkeyEngineRegister.countFactories() > 0);
	}

	public static IHotkeyEngine makeHotkeyEngine () {
		IHotkeyEngine engine = HotkeyEngineRegister.getNewHeykeyEngine();
		if (engine == null) logger.warning("Failed to create HotkeyEngine instance.");
		return engine;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
