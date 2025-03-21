package com.vaguehope.morrigan.sqlitewrapper;

import com.vaguehope.morrigan.model.exceptions.MorriganException;

public class DbException extends MorriganException {

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
