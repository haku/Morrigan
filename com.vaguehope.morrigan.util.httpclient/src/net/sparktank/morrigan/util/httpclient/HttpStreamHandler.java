package net.sparktank.morrigan.util.httpclient;

import java.io.IOException;
import java.io.InputStream;

public interface HttpStreamHandler {
	
	public void handleStream (InputStream is) throws IOException, HttpStreamHandlerException;
	
}
