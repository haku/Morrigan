package morrigan.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import morrigan.util.FileHelper;

public class FileHelperTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void itFreshensOldFile () throws Exception {
		final File f = this.tmp.newFile("a");
		final String data = "" + System.currentTimeMillis() + "." + System.nanoTime();
		FileUtils.writeStringToFile(f, data, Charset.defaultCharset());

		final long lastMod = nowMillis() - TimeUnit.HOURS.toMillis(11);
		assertTrue(f.setLastModified(lastMod));
		assertEquals(lastMod, f.lastModified());

		FileHelper.freshenLastModified(f, 10, TimeUnit.HOURS);
		assertThat(nowMillis() - f.lastModified(), Matchers.lessThan(TimeUnit.SECONDS.toMillis(5)));
		assertEquals(data, FileUtils.readFileToString(f, Charset.defaultCharset()));
	}

	@Test
	public void itDoesNotFreshenNewFile () throws Exception {
		final File f = this.tmp.newFile("a");

		final long lastMod = nowMillis() - TimeUnit.HOURS.toMillis(9);
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
