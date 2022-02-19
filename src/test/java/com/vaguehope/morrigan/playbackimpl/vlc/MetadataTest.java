package com.vaguehope.morrigan.playbackimpl.vlc;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MetadataTest {

	private VlcEngineFactory factory;

	@Before
	public void before() throws Exception {
		this.factory = new VlcEngineFactory();
	}

	@After
	public void after() throws Exception {
		this.factory.dispose();
	}

	@Test
	public void itReadsFileDuration() throws Exception {
		final File file = new File(System.getProperty("user.home") + "/media/testing/test.mp3");
		if (!file.exists()) throw new FileNotFoundException();
		final long d = Metadata.getDurationMilliseconds(this.factory.getMediaPlayerFactory(), file.getAbsolutePath());
		assertTrue(d > 0);
	}

}
