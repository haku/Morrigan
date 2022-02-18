package com.vaguehope.morrigan.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ChecksumHelperTest {

	@Test
	public void itMd5AString () throws Exception {
		for (int i = 0; i < 5; i++) {
			assertEquals("3858f62230ac3c915f300c664312c63f", ChecksumHelper.md5String("foobar"));
		}
	}

}
