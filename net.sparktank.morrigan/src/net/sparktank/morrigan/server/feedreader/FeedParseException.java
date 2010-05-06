package net.sparktank.morrigan.server.feedreader;

import net.sparktank.morrigan.exceptions.MorriganException;

public class FeedParseException extends MorriganException {
	
	private static final long serialVersionUID = 8518574728577998800L;
	
	public FeedParseException() {
	}
	
	public FeedParseException(String s) {
		super(s);
	}
	
	public FeedParseException(String s, Throwable t) {
		super(s, t);
	}
	
	public FeedParseException(Throwable t) {
		super(t);
	}
	
}
