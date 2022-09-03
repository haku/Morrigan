package com.vaguehope.morrigan.util;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import com.vaguehope.morrigan.util.ChecksumHelper.Md5AndSha1;

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

	public BigInteger generateMd5(final File file, final ByteBuffer byteBuffer) throws IOException {
		return ChecksumHelper.generateMd5(file, byteBuffer);
	}

	public Md5AndSha1 generateMd5AndSha1(final File file, final ByteBuffer byteBuffer) throws IOException {
		return ChecksumHelper.generateMd5AndSha1(file, byteBuffer);
	}

}
