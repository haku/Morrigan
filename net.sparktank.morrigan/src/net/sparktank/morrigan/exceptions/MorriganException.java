package net.sparktank.morrigan.exceptions;

public class MorriganException extends Exception {
	
	private static final long serialVersionUID = -6138831951686942856L;

	public MorriganException (String s) {
		super(s);
	}
	
	public MorriganException (String s, Throwable t) {
		super(s, t);
	}
	
}
