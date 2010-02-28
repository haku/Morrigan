package net.sparktank.morrigan.engines;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.engines.hotkey.HotkeyException;
import net.sparktank.morrigan.engines.hotkey.HotkeyValue;
import net.sparktank.morrigan.engines.hotkey.IHotkeyEngine;
import net.sparktank.morrigan.engines.hotkey.IHotkeyListener;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.preferences.HotkeyPref;

public class HotkeyRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final int HK_PLAYPAUSE = 100;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	synchronized public static void addHotkeyListener (IHotkeyListener listener) throws MorriganException {
		readConfig(false);
		if (!hotkeyListeners.contains(listener)) {
			hotkeyListeners.add(listener);
		}
	}
	
	synchronized public static void removeHotkeyListener (IHotkeyListener listener) throws MorriganException {
		if (hotkeyListeners.contains(listener)) {
			hotkeyListeners.remove(listener);
		}
		if (hotkeyListeners.size()<1) {
			clearConfig();
			clearHotkeyEngine();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static boolean configRead = false;
	
	synchronized public static void readConfig (boolean force) throws MorriganException {
		if (configRead && !force) return;
		
		clearConfig();
		
		HotkeyValue hkPlaypause = HotkeyPref.getHkPlaypause();
		if (hkPlaypause!=null) {
			IHotkeyEngine engine = getHotkeyEngine(true);
			engine.registerHotkey(HK_PLAYPAUSE, hkPlaypause);
			System.out.println("registered HK_PLAYPAUSE: " + hkPlaypause.toString());
		}
		
		configRead = true;
	}
	
	private static void clearConfig () throws HotkeyException, ImplException {
		IHotkeyEngine engine = getHotkeyEngine(false);
		if (engine!=null) {
			engine.unregisterHotkey(HK_PLAYPAUSE);
			System.out.println("unregistered HK_PLAYPAUSE.");
		}
		configRead = false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static IHotkeyEngine hotkeyEngine = null;
	private static List<IHotkeyListener> hotkeyListeners = new ArrayList<IHotkeyListener>();
	
	private static IHotkeyEngine getHotkeyEngine (boolean create) throws ImplException {
		if (hotkeyEngine == null && create) {
			hotkeyEngine = EngineFactory.makeHotkeyEngine();
			hotkeyEngine.setListener(mainHotkeyListener);
		}
		
		return hotkeyEngine;
	}
	
	private static void clearHotkeyEngine () {
		if (hotkeyEngine!=null) {
			hotkeyEngine.finalise();
			hotkeyEngine = null;
		}
	}
	
	private static IHotkeyListener mainHotkeyListener = new IHotkeyListener () {
		@Override
		public void onKeyPress(int id) {
			for (IHotkeyListener l : hotkeyListeners) {
				l.onKeyPress(id);
			}
		}
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
