package net.sparktank.morrigan.engines.hotkey;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HotkeyEngineRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private ConcurrentMap<String, HotkeyEngineFactory> engineFactories = new ConcurrentHashMap<String, HotkeyEngineFactory>();
	
	static public void registerFactory (String id, HotkeyEngineFactory factory) {
		HotkeyEngineFactory r = engineFactories.putIfAbsent(id, factory);
		if (r != null) throw new IllegalArgumentException("ID '"+id+"' already in use.");
	}
	
	static public void unregisterFactory (String id) {
		engineFactories.remove(id);
	}
	
	/**
	 * TODO add much better error handling.
	 * @return
	 */
	static public IHotkeyEngine getNewHeykeyEngine () {
		if (engineFactories.size() < 1) {
			return null;
		}
		
		for (Entry<String, HotkeyEngineFactory> entry : engineFactories.entrySet()) {
			try {
				IHotkeyEngine engine = entry.getValue().getNewHotkeyEngine();
				return engine;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
