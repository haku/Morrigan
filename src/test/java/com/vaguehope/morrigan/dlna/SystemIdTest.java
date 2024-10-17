package com.vaguehope.morrigan.dlna;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SystemIdTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void itGeneratesWithoutFile() throws Exception {
		final SystemId undertest = new SystemId(null);
		assertNotNull(undertest.getUsi("Service"));
	}

	@Test
	public void itWritesAndReadsFile() throws Exception {
		final File f = this.tmp.newFile();

		final byte[] highSeed = new byte[6];
		new Random().nextBytes(highSeed);

		final SystemId undertest0 = new SystemId(f, () -> highSeed);
		final String actual0 = undertest0.getUsi("Service").getIdentifierString();

		highSeed[0] += 1;
		final SystemId undertest1 = new SystemId(f, () -> highSeed);
		final String actual1 = undertest1.getUsi("Service").getIdentifierString();
		assertEquals(actual0, actual1);

		final String actual2 = undertest1.getUsi("Service2").getIdentifierString();
		assertNotEquals(actual0, actual2);
	}

}
