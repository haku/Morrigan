package com.vaguehope.morrigan.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.util.ChecksumHelper.Md5AndSha1;

public class ChecksumHelperTest {

	private static final String FOOBAR_MD5 = "3858f62230ac3c915f300c664312c63f";
	private static final String FOOBAR_SHA1 = "8843d7f92416211de9ebb963ff4ce28125932878";
	private static final String FOOBAR_100000_MD5 = "f8d9ee8e3de45e512b79e4cbc0c2f83";
	private static final String FOOBAR_100000_SHA1 = "64698dc1315d0a665e7072bc248693d9fe384d7d";

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void itMd5AString () throws Exception {
		for (int i = 0; i < 5; i++) {
			assertEquals(FOOBAR_MD5, ChecksumHelper.md5String("foobar"));
		}
	}

	@Test
	public void itMd5AFile() throws Exception {
		final File f = this.tmp.newFile();
		FileUtils.writeStringToFile(f, "foobar", StandardCharsets.UTF_8);
		final BigInteger actual = ChecksumHelper.generateMd5(f, ChecksumHelper.createByteBuffer());
		assertEquals(FOOBAR_MD5, actual.toString(16));
	}

	@Test
	public void itMd5AndSha1AFile() throws Exception {
		final File f = this.tmp.newFile();
		FileUtils.writeStringToFile(f, "foobar", StandardCharsets.UTF_8);
		final Md5AndSha1 actual = ChecksumHelper.generateMd5AndSha1(f, ChecksumHelper.createByteBuffer());
		assertEquals(FOOBAR_MD5, actual.getMd5().toString(16));
		assertEquals(FOOBAR_SHA1, actual.getSha1().toString(16));
	}

	@Test
	public void itMd5AndSha1ALongerFile() throws Exception {
		final File f = this.tmp.newFile();
		final byte[] foobarBytes = "foobar".getBytes(StandardCharsets.UTF_8);
		try (OutputStream s = new FileOutputStream(f)) {
			for (int i = 0; i < 100000; i++) {
				s.write(foobarBytes);
			}
		}
		assertThat((int) f.length(), greaterThan(ChecksumHelper.BUFFERSIZE));

		final Md5AndSha1 actual = ChecksumHelper.generateMd5AndSha1(f, ChecksumHelper.createByteBuffer());
		assertEquals(FOOBAR_100000_MD5, actual.getMd5().toString(16));
		assertEquals(FOOBAR_100000_SHA1, actual.getSha1().toString(16));
	}

}
