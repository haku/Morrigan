package com.vaguehope.morrigan.playbackimpl.vlc;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.vaguehope.morrigan.Args;

public class MetadataTest {

	private Args args;
	private VlcEngineFactory factory;

	@Before
	public void before() throws Exception {
		this.args = mock(Args.class);
		this.factory = new VlcEngineFactory(this.args);
	}

	@After
	public void after() throws Exception {
		this.factory.dispose();
	}

	@Ignore
	@Test
	public void itReadsFileDuration() throws Exception {
		final File file = new File(System.getProperty("user.home") + "/media/testing/test.mp3");
		if (!file.exists()) throw new FileNotFoundException();
		final long d = Metadata.getDurationMilliseconds(this.factory.getMediaPlayerFactory(), file.getAbsolutePath());
		assertTrue(d > 0);
	}

}
