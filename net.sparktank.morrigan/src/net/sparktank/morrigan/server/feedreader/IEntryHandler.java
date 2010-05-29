package net.sparktank.morrigan.server.feedreader;

import org.w3c.dom.Node;

public interface IEntryHandler {
	
	public abstract void parseEntry (Node entry) throws FeedParseException;
	
}
