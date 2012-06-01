package com.vaguehope.morrigan.sshplayer.omxplayer;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class OmxplayerStatusReaderTest {

	@Test
	public void testReadPosition () throws Exception {
		final String data = "V :     3.12  4915200  4915200 A :     3.65     1.98 Cv :  4482284 Ca :   200448\n";

		OmxplayerStatusReader p = new OmxplayerStatusReader(new ByteArrayInputStream(data.getBytes()));
		p.start();
		p.join();

		assertEquals(3, p.getCurrentPosition());
	}
	
}
