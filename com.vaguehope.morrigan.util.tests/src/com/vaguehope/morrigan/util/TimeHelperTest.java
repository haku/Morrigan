package com.vaguehope.morrigan.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class TimeHelperTest {

	@Test
	public void itParsesDurations () throws Exception {
		testParseDuration(0, "0");
		testParseDuration(0, "0:0");
		testParseDuration(0, "0:0:0");
		testParseDuration(1, "1");
		testParseDuration(12345, "12345");
		testParseDuration(62, "1:2");
		testParseDuration(3723, "1:2:3");
		testParseDuration(1902, "31:42");
		testParseDuration(17583, "4:52:63");
		testParseDuration(3723, " 1 : 2 : 3 ");
	}

	@Test
	public void itHandlesInvalid () throws Exception {
		assertEquals(null, TimeHelper.parseDuration("a"));
		assertEquals(null, TimeHelper.parseDuration("a:b"));
		assertEquals(null, TimeHelper.parseDuration("a:1"));
	}

	private void testParseDuration (final long expected, final String actual) {
		assertEquals(Long.valueOf(expected), TimeHelper.parseDuration(actual));
	}

}
