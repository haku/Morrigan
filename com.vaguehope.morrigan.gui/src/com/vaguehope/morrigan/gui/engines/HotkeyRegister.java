package com.vaguehope.morrigan.gui.engines;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.vaguehope.morrigan.engines.hotkey.HotkeyEngineFactory;
import com.vaguehope.morrigan.engines.hotkey.HotkeyException;
import com.vaguehope.morrigan.engines.hotkey.HotkeyValue;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyEngine;
import com.vaguehope.morrigan.engines.hotkey.IHotkeyListener;
import com.vaguehope.morrigan.gui.preferences.HotkeyPref;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.util.MnLogger;

public class HotkeyRegister {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final MnLogger LOG = MnLogger.make(HotkeyRegister.class);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final HotkeyEngineFactory hotkeyEngineFactory;

	protected final ConcurrentMap<IHotkeyListener, Object> hotkeyListeners = new ConcurrentHashMap<IHotkeyListener, Object>();

	public HotkeyRegister (final HotkeyEngineFactory hotkeyEngineFactory) {
		this.hotkeyEngineFactory = hotkeyEngineFactory;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void addHotkeyListener (final IHotkeyListener listener) throws MorriganException {
		this.hotkeyListeners.put(listener, listener);
		LOG.d("Hotkey listener registered: '{}'.", listener);
		readConfig(false);
	}

	public void removeHotkeyListener (final IHotkeyListener listener) throws MorriganException {
		this.hotkeyListeners.remove(listener);
		if (this.hotkeyListeners.size() < 1) {
			clearConfig();
			clearHotkeyEngine();
		}
	}

	/**
	 * @param id one of IHotkeyEngine.MORRIGAN_HK_*
	 */
	public void sendHotkeyEvent(final int id) {
		this.mainHotkeyListener.onKeyPress(id);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final AtomicBoolean configRead = new AtomicBoolean(false);
	private final List<Integer> registeredHotkeys = new ArrayList<Integer>();

	@SuppressWarnings("boxing")
	public void readConfig (final boolean force) throws MorriganException {
		if (!this.configRead.compareAndSet(false, true) & !force) {
			LOG.d("Hotkey config already read, skipping.");
			return;
		}

		clearConfig();

		if (this.hotkeyEngineFactory.canMakeHotkeyEngine()) {
			HotkeyValue hkShowHide = HotkeyPref.getHkShowHide();
			if (hkShowHide!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE, hkShowHide);
				this.registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE);
				LOG.d("registered MORRIGAN_HK_SHOWHIDE: {}", hkShowHide.toString());
			}

			HotkeyValue hkStop = HotkeyPref.getHkStop();
			if (hkStop!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_STOP, hkStop);
				this.registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_STOP);
				LOG.d("registered MORRIGAN_HK_STOP: {}", hkStop.toString());
			}

			HotkeyValue hkPlaypause = HotkeyPref.getHkPlaypause();
			if (hkPlaypause!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE, hkPlaypause);
				this.registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE);
				LOG.d("registered MORRIGAN_HK_PLAYPAUSE: {}", hkPlaypause.toString());
			}

			HotkeyValue hkNext = HotkeyPref.getHkNext();
			if (hkNext!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_NEXT, hkNext);
				this.registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_NEXT);
				LOG.d("registered MORRIGAN_HK_NEXT: {}", hkNext.toString());
			}

			HotkeyValue hkJumpto = HotkeyPref.getHkJumpto();
			if (hkJumpto!=null) {
				getHotkeyEngine(true).registerHotkey(IHotkeyEngine.MORRIGAN_HK_JUMPTO, hkJumpto);
				this.registeredHotkeys.add(IHotkeyEngine.MORRIGAN_HK_JUMPTO);
				LOG.d("registered MORRIGAN_HK_JUMPTO: {}", hkJumpto.toString());
			}
		}
		else {
			LOG.w("EngineFactory.canMakeHotkeyEngine() == false.");
		}
	}

	@SuppressWarnings("boxing")
	private void clearConfig () throws HotkeyException {
		IHotkeyEngine engine = getHotkeyEngine(false);

		if (engine != null) {
			LOG.d("Going to unregister hotkeys...");

			if (this.registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE)) {
				LOG.d("Going to unregister MORRIGAN_HK_SHOWHIDE...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE);
				this.registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_SHOWHIDE));
				LOG.d("unregistered MORRIGAN_HK_SHOWHIDE.");
			}

			if (this.registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_STOP)) {
				LOG.d("Going to unregister MORRIGAN_HK_STOP...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_STOP);
				this.registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_STOP));
				LOG.d("unregistered MORRIGAN_HK_STOP.");
			}

			if (this.registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE)) {
				LOG.d("Going to unregister MORRIGAN_HK_PLAYPAUSE...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE);
				this.registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_PLAYPAUSE));
				LOG.d("unregistered MORRIGAN_HK_PLAYPAUSE.");
			}

			if (this.registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_NEXT)) {
				LOG.d("Going to unregister MORRIGAN_HK_NEXT...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_NEXT);
				this.registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_NEXT));
				LOG.d("unregistered MORRIGAN_HK_NEXT.");
			}

			if (this.registeredHotkeys.contains(IHotkeyEngine.MORRIGAN_HK_JUMPTO)) {
				LOG.d("Going to unregister MORRIGAN_HK_JUMPTO...");
				engine.unregisterHotkey(IHotkeyEngine.MORRIGAN_HK_JUMPTO);
				this.registeredHotkeys.remove(Integer.valueOf(IHotkeyEngine.MORRIGAN_HK_JUMPTO));
				LOG.d("unregistered MORRIGAN_HK_JUMPTO.");
			}
		}

		this.configRead.set(false);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final AtomicReference<IHotkeyEngine> hotkeyEngine = new AtomicReference<IHotkeyEngine>(null);

	private IHotkeyEngine getHotkeyEngine (final boolean create) {
		if (hotkeyEngine.get() == null && create) {
			IHotkeyEngine e = this.hotkeyEngineFactory.newHotkeyEngine();
			if (e != null) {
				if (hotkeyEngine.compareAndSet(null, e)) {
					e.setListener(this.mainHotkeyListener);
				}
				else {
					e.dispose();
				}
			}
		}
		return hotkeyEngine.get();
	}

	private static void clearHotkeyEngine () {
		IHotkeyEngine e = hotkeyEngine.getAndSet(null);
		if (e != null) e.dispose();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final IHotkeyListener mainHotkeyListener = new IHotkeyListener () {

		private WeakReference<IHotkeyListener> lastIHotkeyListenerUsed = null;

		@Override
		public void onKeyPress(final int id) {
			List<IHotkeyListener> answers = new ArrayList<IHotkeyListener>();

			IHotkeyListener last = null;
			if (this.lastIHotkeyListenerUsed != null) {
				last = this.lastIHotkeyListenerUsed.get();
			}

			for (IHotkeyListener l : HotkeyRegister.this.hotkeyListeners.keySet()) {
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
						// FIXME What if the next answer is YES?
						answers.add(l);
					}
				}
			}

			if (!answers.isEmpty()) {
				for (IHotkeyListener l : answers) {
					try {
						l.onKeyPress(id);
					}
					catch (final Exception e) {
						LOG.e("Exception handling hotkey event.", e);
					}
				}

				if (answers.size() == 1) {
					this.lastIHotkeyListenerUsed = new WeakReference<IHotkeyListener>(answers.get(0));
				}
			}
			else {
				LOG.w("Failed to find handler for hotkey cmd '{}'.", id);
			}
		}

		@Override
		public CanDo canDoKeyPress(final int id) {
			return CanDo.NO;
		}

	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
