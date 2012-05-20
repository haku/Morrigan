package com.vaguehope.morrigan.sshplayer;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class MplayerStatusReaderTest {

	@Test
	public void testReadDuration () throws Exception {
		final String DATA = "\n\nID_AUDIO_RATE=0\n" +
				"ID_AUDIO_NCH=0\n" +
				"ID_START_TIME=0.00\n" +
				"ID_LENGTH=303.10\n" +
				"ID_SEEKABLE=1\n" +
				"ID_CHAPTERS=0\n" +
				"open: No such file or directory\n" +
				"[MGA] Couldn't open: /dev/mga_vid\n" +
				"open: No such file or directory\n" +
				"[MGA] Couldn't open: /dev/mga_vid\n\n";

		MplayerStatusReader p = new MplayerStatusReader(new ByteArrayInputStream(DATA.getBytes()));
		p.run();

		assertEquals(303, p.getDuration());
	}

	@Test
	public void testReadPosition () throws Exception {
		final String DATA = "A:   1.6 V:   1.7 A-V: -0.031 ct: -0.023  51/ 51 19%  8%  0.4% 0 0 96%          \n" +
				"A:   1.7 V:   1.7 A-V: -0.044 ct: -0.027  52/ 52 19%  8%  0.4% 0 0 96%          \n" +
				"A:   1.8 V:   1.8 A-V: -0.009 ct: -0.031  55/ 55 18%  7%  0.4% 0 0 96%          \n" +
				"A:   1.9 V:   1.9 A-V: -0.001 ct: -0.034  58/ 58 18%  7%  0.4% 0 0 96%          \n" +
				"A:   2.0 V:   2.0 A-V: -0.043 ct: -0.042  61/ 61 17%  7%  0.5% 0 0 96%    \n";

		MplayerStatusReader p = new MplayerStatusReader(new ByteArrayInputStream(DATA.getBytes()));
		p.run();

		assertEquals(2, p.getCurrentPosition());
	}

}
