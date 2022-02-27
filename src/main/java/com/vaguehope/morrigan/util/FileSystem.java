package com.vaguehope.morrigan.util;

import java.io.File;

/**
 * To make testing and mocking easier.
 */
public class FileSystem {

	public File makeFile(final String pathname) {
		return new File(pathname);
	}

	public File makeFile(final File parent, final String child) {
		return new File(parent, child);
	}

}
