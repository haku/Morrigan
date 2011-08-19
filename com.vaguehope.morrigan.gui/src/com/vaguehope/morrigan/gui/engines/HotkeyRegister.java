package com.vaguehope.morrigan.gui.engines;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import com.vaguehope.morrigan.engines.EngineFactory;
import com.vaguehope.morrigan.engines.common.ImplException;
import com.vaguehope.morrigan.engines.hotkey.HotkeyException;
import com.vaguehope.morrigan.engines.hotkey.HotkeyValue;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyEngine;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyListener;
import com.vaguehope.morrigan.gui.preferences.HotkeyPref;
import com.vaguehope.morrigan.model.exceptions.MorriganException;


public class HotkeyRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected static final Logger logger = Logger.getLogger(HotkeyRegister.class.getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static protected final ConcurrentMap<IHotkeyListener, Object> hotkeyListeners = new ConcurrentHashMap<IHotkeyListener, Object>();
	
	public static void addHotkeyListener (IHotkeyListener listener) throws MorriganException {
		hotkeyListeners.put(listener, listener);
		logger.fine("Hotkey listener registered: '"+listener+"'.");
		readConfig(false);
	}
	
	public static void removeHotkeyListener (IHotkeyListener listener) throws MorriganException {
		hotkeyListeners.remove(listener);
		if (hotkeyListeners.size() < 1) {
			clearConfig();
			clearHotkeyEngine();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private final AtomicBoolean configRead = new AtomicBoolean(false);
	private static List<Integer> registeredHotkeys = new ArrayList<Integer>();
	
	@SuppressWarnings("boxing")
	public static void readConfig (boolean force) throws MorriganException {
		if (!configRead.compareAndSet(false, true) & !force) {
			logger.fine("Hotkey config already read, skipping.");
			return;
		}
		
		clearConfig();
		
		if (EngineFactory.canMakeHotkeyEngine()) {
			HotkeyValue hkShowHide = HotkeyPref.getHkShowHide();
			if (hkShowHide!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE, hkShowHide);
				registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE);
				logger.fine("registered MORRIGAN_HK_SHOWHIDE: " + hkShowHide.toString());
			}
			
			HotkeyValue hkStop = HotkeyPref.getHkStop();
			if (hkStop!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_STOP, hkStop);
				registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_STOP);
				logger.fine("registered MORRIGAN_HK_STOP: " + hkStop.toString());
			}
			
			HotkeyValue hkPlaypause = HotkeyPref.getHkPlaypause();
			if (hkPlaypause!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE, hkPlaypause);
				registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE);
				logger.fine("registered MORRIGAN_HK_PLAYPAUSE: " + hkPlaypause.toString());
			}
			
			HotkeyValue hkNext = HotkeyPref.getHkNext();
			if (hkNext!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_NEXT, hkNext);
				registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_NEXT);
				logger.fine("registered MORRIGAN_HK_NEXT: " + hkNext.toString());
			}
			
			HotkeyValue hkJumpto = HotkeyPref.getHkJumpto();
			if (hkJumpto!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_JUMPTO, hkJumpto);
				registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_JUMPTO);
				logger.fine("registered MORRIGAN_HK_JUMPTO: " + hkJumpto.toString());
			}
		}
		else {
			logger.warning("EngineFactory.canMakeHotkeyEngine() == false.");
		}
	}
	
	@SuppressWarnings("boxing")
	private static void clearConfig () throws HotkeyException, ImplException {
		IHotkeyEngine engine = getHotkeyEngine(false);
		
		if (engine != null) {
			logger.fine("Going to unregister hotkeys...");
			
			if (registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE)) {
				logger.fine("Going to unregister MORRIGAN_HK_SHOWHIDE...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE);
				registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE));
				logger.fine("unregistered MORRIGAN_HK_SHOWHIDE.");
			}
			
			if (registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_STOP)) {
				logger.fine("Going to unregister MORRIGAN_HK_STOP...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_STOP);
				registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_STOP));
				logger.fine("unregistered MORRIGAN_HK_STOP.");
			}
			
			if (registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE)) {
				logger.fine("Going to unregister MORRIGAN_HK_PLAYPAUSE...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE);
				registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE));
				logger.fine("unregistered MORRIGAN_HK_PLAYPAUSE.");
			}
			
			if (registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_NEXT)) {
				logger.fine("Going to unregister MORRIGAN_HK_NEXT...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_NEXT);
				registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_NEXT));
				logger.fine("unregistered MORRIGAN_HK_NEXT.");
			}
			
			if (registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_JUMPTO)) {
				logger.fine("Going to unregister MORRIGAN_HK_JUMPTO...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_JUMPTO);
				registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_JUMPTO));
				logger.fine("unregistered MORRIGAN_HK_JUMPTO.");
			}
		}
		
		configRead.set(false);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private final AtomicReference<IHotkeyEngine> hotkeyEngine = new AtomicReference<IHotkeyEngine>(null);
	
	private static IHotkeyEngine getHotkeyEngine (boolean create) throws ImplException {
		if (hotkeyEngine.get() == null && create) {
			IHotkeyEngine e = EngineFactory.makeHotkeyEngine();
			if (e != null) {
				if (hotkeyEngine.compareAndSet(null, e)) {
					e.setListener(mainHotkeyListener);
				}
				else {
					e.finalise();
				}
			}
		}
		
		return hotkeyEngine.get();
	}
	
	private static void clearHotkeyEngine () {
		IHotkeyEngine e = hotkeyEngine.getAndSet(null);
		if (e != null) e.finalise();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static IHotkeyListener mainHotkeyListener = new IHotkeyListener () {
		
		private WeakReference<IHotkeyListener> lastIHotkeyListenerUsed = null;
		
		@Override
		public void onKeyPress(int id) {
			List<IHotkeyListener> answers = new ArrayList<IHotkeyListener>();
			
			IHotkeyListener last = null;
			if (this.lastIHotkeyListenerUsed != null) {
				last = this.lastIHotkeyListenerUsed.get();
			}
			
			for (IHotkeyListener l : hotkeyListeners.keySet()) {
				CanDo canDo = l.canDoKeyPress(id);
				
				if (canDo == CanDo.YESANDFRIENDS) {
					answers.add(l);
				}
				else if (canDo == CanDo.YES) {
					answers.add(l);
					break;
				}
				else if (canDo == CanDo.MAYBE) {
					if (l == last) {
						answers.add(l);
					}
					else if (answers.isEmpty()) {
						answers.add(l);
					}
				}
			}
			
			if (!answers.isEmpty()) {
				for (IHotkeyListener l : answers) {
					l.onKeyPress(id);
				}
				
				if (answers.size() == 1) {
					this.lastIHotkeyListenerUsed = new WeakReference<IHotkeyListener>(answers.get(0));
				}
			}
			else {
				logger.warning("Failed to find handler for hotkey cmd '"+id+"'.");
			}
		}
		
		@Override
		public CanDo canDoKeyPress(int id) {
			return CanDo.NO;
		}
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
