package com.vaguehope.morrigan.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;

import org.junit.Test;

public class IoHelperTest {

	@Test
	public void itSkipsAll () throws Exception {
		final InputStream is = new BufferedInputStream(new ByteArrayInputStream(new byte[1024 * 1024 * 10]));
		IoHelper.skipReliably(is, 1024 * 1024);
		assertEquals(1024 * 1024 * 9, IoHelper.drainStream(is));
	}

	@Test
	public void itThrowsIfNotEnoughToSkip () throws Exception {
		final InputStream is = new BufferedInputStream(new ByteArrayInputStream(new byte[1024 * 1024 * 10]));
		try {
			IoHelper.skipReliably(is, (1024 * 1024 * 10) + 1);
			fail("Expected a throw.");
		}
		catch (EOFException e) {
			// Expected.
		}
	}

}
