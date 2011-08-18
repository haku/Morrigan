package net.sparktank.morrigan.engines.common;

import net.sparktank.morrigan.model.exceptions.MorriganException;

public class ImplException extends MorriganException {
	
	private static final long serialVersionUID = 4396988400427258464L;

	public ImplException (Throwable t) {
		super(t);
	}
	
}
