package net.sparktank.sqlitewrapper;

public class DbException extends Exception {
	
	private static final long serialVersionUID = 9079295012945637115L;

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
