package com.vaguehope.morrigan.dlna.httpserver;

import java.io.File;
import java.io.IOException;

public interface FileLocator {

	// Returns null for not found.
	File idToFile(String id) throws IOException;

}
