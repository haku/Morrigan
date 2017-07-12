package com.vaguehope.morrigan.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileHelperTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void itFreshensOldFile () throws Exception {
		final File f = this.tmp.newFile("a");

		long lastMod = nowMillis() - TimeUnit.HOURS.toMillis(11);
		assertTrue(f.setLastModified(lastMod));
		assertEquals(lastMod, f.lastModified());

		FileHelper.freshenLastModified(f, 10, TimeUnit.HOURS);
		assertTrue(nowMillis() - f.lastModified() < TimeUnit.SECONDS.toMillis(5));
	}

	@Test
	public void itDoesNotFreshenNewFile () throws Exception {
		final File f = this.tmp.newFile("a");

		long lastMod = nowMillis() - TimeUnit.HOURS.toMillis(9);
		assertTrue(f.setLastModified(lastMod));
		assertEquals(lastMod, f.lastModified());

		FileHelper.freshenLastModified(f, 10, TimeUnit.HOURS);
		assertEquals(lastMod, f.lastModified());
	}

	/**
	 * Rounded to nearest second.
	 */
	private static long nowMillis () {
		return System.currentTimeMillis() / 1000 * 1000;
	}
}
