package net.sparktank.nemain.model;

public class NemainException extends Exception {
	
	private static final long serialVersionUID = -4285110456878964140L;
	
	public NemainException() {/* UNUSED */}
	
	public NemainException (String s) {
		super(s);
	}
	
	public NemainException (String s, Throwable t) {
		super(s, t);
	}

	public NemainException(Throwable t) {
		super(t);
	}
	
}
