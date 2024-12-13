package com.vaguehope.morrigan.transcode;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class FfprobeParserTest {

	private FfprobeParser undertest;

	@Before
	public void before() throws Exception {
		this.undertest = new FfprobeParser(0L);
	}

	@Test
	public void itHandlesMixedDurations1() throws Exception {
		this.undertest.onAnswer("streams.stream.0.duration=\"15.435200\"");
		this.undertest.onAnswer("streams.stream.1.duration=\"15.401792\"");
		this.undertest.onAnswer("streams.stream.2.duration=\"15.435200\"");
		this.undertest.onAnswer("streams.stream.3.duration=\"15.435200\"");
		assertEquals(15435L, this.undertest.build().getDurationMillis().longValue());
	}

	@Test
	public void itHandlesMixedDurations2() throws Exception {
		this.undertest.onAnswer("streams.stream.0.duration=\"N/A\"");
		this.undertest.onAnswer("streams.stream.0.tags.DURATION=\"00:03:47.000000000\"");
		this.undertest.onAnswer("streams.stream.1.duration=\"N/A\"");
		this.undertest.onAnswer("streams.stream.1.tags.DURATION=\"00:03:47.041000000\"");
		assertEquals(227000L, this.undertest.build().getDurationMillis().longValue());
	}

	@Test
	public void itHandlesMixedDurations3() throws Exception {
		this.undertest.onAnswer("streams.stream.0.duration=\"212.007500\"");
		this.undertest.onAnswer("streams.stream.0.tags.DURATION=\"00:05:15.821000000\"");
		assertEquals(212007L, this.undertest.build().getDurationMillis().longValue());
	}

}
