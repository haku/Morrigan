package net.sparktank.morrigan.engines.hotkey;

import net.sparktank.morrigan.model.exceptions.MorriganException;

public class HotkeyException extends MorriganException {
	
	private static final long serialVersionUID = 5099155004890575600L;
	
	public HotkeyException () {
		super();
	}
	
	public HotkeyException (String s) {
		super(s);
	}
	
	public HotkeyException (String s, Throwable t) {
		super(s, t);
	}
	
	public HotkeyException(Throwable t) {
		super(t);
	}
	
}
