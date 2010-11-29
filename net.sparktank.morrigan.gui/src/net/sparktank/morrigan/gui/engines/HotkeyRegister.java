package net.sparktank.morrigan.gui.engines;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.engines.hotkey.HotkeyException;
import net.sparktank.morrigan.engines.hotkey.HotkeyValue;
import net.sparktank.morrigan.engines.hotkey.IHotkeyEngine;
import net.sparktank.morrigan.engines.hotkey.IHotkeyListener;
import net.sparktank.morrigan.gui.preferences.HotkeyPref;
import net.sparktank.morrigan.model.exceptions.MorriganException;

public class HotkeyRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	synchronized public static void addHotkeyListener (IHotkeyListener listener) throws MorriganException {
		if (!hotkeyListeners.contains(listener)) {
			readConfig(false);
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
	private static List<Integer> registeredHotkeys = new ArrayList<Integer>();
	
	@SuppressWarnings("boxing")
	synchronized public static void readConfig (boolean force) throws MorriganException {
		if (configRead && !force) return;
		
		clearConfig();
		
		if (EngineFactory.canMakeHotkeyEngine()) {
			HotkeyValue hkShowHide = HotkeyPref.getHkShowHide();
			if (hkShowHide!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE, hkShowHide);
				registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE);
				System.out.println("registered MORRIGAN_HK_SHOWHIDE: " + hkShowHide.toString());
			}
			
			HotkeyValue hkStop = HotkeyPref.getHkStop();
			if (hkStop!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_STOP, hkStop);
				registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_STOP);
				System.out.println("registered MORRIGAN_HK_STOP: " + hkStop.toString());
			}
			
			HotkeyValue hkPlaypause = HotkeyPref.getHkPlaypause();
			if (hkPlaypause!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE, hkPlaypause);
				registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE);
				System.out.println("registered MORRIGAN_HK_PLAYPAUSE: " + hkPlaypause.toString());
			}
			
			HotkeyValue hkNext = HotkeyPref.getHkNext();
			if (hkNext!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_NEXT, hkNext);
				registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_NEXT);
				System.out.println("registered MORRIGAN_HK_NEXT: " + hkNext.toString());
			}
			
			HotkeyValue hkJumpto = HotkeyPref.getHkJumpto();
			if (hkJumpto!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_JUMPTO, hkJumpto);
				registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_JUMPTO);
				System.out.println("registered MORRIGAN_HK_JUMPTO: " + hkJumpto.toString());
			}
		}
		
		configRead = true;
	}
	
	@SuppressWarnings("boxing")
	private static void clearConfig () throws HotkeyException, ImplException {
		IHotkeyEngine engine = getHotkeyEngine(false);
		
		if (engine!=null) {
			System.out.println("Going to unregister hotkeys...");
			
			if (registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE)) {
				System.out.println("Going to unregister MORRIGAN_HK_SHOWHIDE...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE);
				registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE));
				System.out.println("unregistered MORRIGAN_HK_SHOWHIDE.");
			}
			
			if (registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_STOP)) {
				System.out.println("Going to unregister MORRIGAN_HK_STOP...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_STOP);
				registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_STOP));
				System.out.println("unregistered MORRIGAN_HK_STOP.");
			}
			
			if (registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE)) {
				System.out.println("Going to unregister MORRIGAN_HK_PLAYPAUSE...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE);
				registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE));
				System.out.println("unregistered MORRIGAN_HK_PLAYPAUSE.");
			}
			
			if (registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_NEXT)) {
				System.out.println("Going to unregister MORRIGAN_HK_NEXT...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_NEXT);
				registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_NEXT));
				System.out.println("unregistered MORRIGAN_HK_NEXT.");
			}
			
			if (registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_JUMPTO)) {
				System.out.println("Going to unregister MORRIGAN_HK_JUMPTO...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_JUMPTO);
				registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_JUMPTO));
				System.out.println("unregistered MORRIGAN_HK_JUMPTO.");
			}
		}
		
		configRead = false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static IHotkeyEngine hotkeyEngine = null;
	static List<IHotkeyListener> hotkeyListeners = new ArrayList<IHotkeyListener>();
	
	private static IHotkeyEngine getHotkeyEngine (boolean create) throws ImplException {
		if (hotkeyEngine == null && create) {
			hotkeyEngine = EngineFactory.makeHotkeyEngine();
			if (hotkeyEngine != null) {
				hotkeyEngine.setListener(mainHotkeyListener);
			}
		}
		
		return hotkeyEngine;
	}
	
	private static void clearHotkeyEngine () {
		if (hotkeyEngine!=null) {
			hotkeyEngine.finalise();
			hotkeyEngine = null;
		}
	}
	
	static WeakReference<IHotkeyListener> lastIHotkeyListenerUsed = null;
	
	private static IHotkeyListener mainHotkeyListener = new IHotkeyListener () {
		
		@Override
		public void onKeyPress(int id) {
			List<IHotkeyListener> answers = new ArrayList<IHotkeyListener>();
			
			IHotkeyListener last = null;
			if (lastIHotkeyListenerUsed != null) {
				last = lastIHotkeyListenerUsed.get();
			}
			
			for (IHotkeyListener l : hotkeyListeners) {
				CanDo canDo = l.canDoKeyPress(id);
				
				if (canDo == CanDo.YESANDFRIENDS) {
					answers.add(l);
					
				} else if (canDo == CanDo.YES) {
					answers.add(l);
					break;
					
				} else if (canDo == CanDo.MAYBE) {
					if (l == last) {
						answers.add(l);
					} else if (answers.isEmpty()) {
						answers.add(l);
					}
				}
			}
			
			if (!answers.isEmpty()) {
				for (IHotkeyListener l : answers) {
					l.onKeyPress(id);
				}
				
				if (answers.size() == 1) {
					lastIHotkeyListenerUsed = new WeakReference<IHotkeyListener>(answers.get(0));
				}
				
			} else {
				System.err.println("Failed to find handler for hotkey cmd '"+id+"'.");
			}
		}
		
		@Override
		public CanDo canDoKeyPress(int id) {
			return CanDo.NO;
		}
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
