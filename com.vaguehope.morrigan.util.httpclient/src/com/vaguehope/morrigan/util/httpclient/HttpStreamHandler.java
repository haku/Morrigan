package com.vaguehope.morrigan.util.httpclient;

import java.io.IOException;
import java.io.InputStream;

public interface HttpStreamHandler {

	void handleStream (InputStream is) throws IOException, HttpStreamHandlerException;

}
