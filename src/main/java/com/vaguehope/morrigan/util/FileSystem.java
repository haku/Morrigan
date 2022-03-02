package com.vaguehope.morrigan.util;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

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

	public BigInteger generateMd5Checksum(File file, ByteBuffer byteBuffer) throws IOException {
		return ChecksumHelper.generateMd5Checksum(file, byteBuffer);
	}

}
