package net.sparktank.morrigan.model.library;

import net.sparktank.morrigan.exceptions.MorriganException;

public class DbException extends MorriganException {
	
	private static final long serialVersionUID = -1896813172762587041L;
	
	public DbException (String s) {
		super(s);
	}
	
	public DbException (String s, Throwable t) {
		super(s, t);
	}
	
	public DbException(Throwable t) {
		super(t);
	}
	
}
