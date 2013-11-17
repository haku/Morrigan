package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDb;

public class MixedMediaSqliteLayerOuterTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	private File dbFile;

	private MixedMediaSqliteLayerOuter undertest;

	@Before
	public void before () throws Exception {
		this.dbFile = this.tmp.newFile("testdb.db3");
		this.undertest = new MixedMediaSqliteLayerOuter(this.dbFile.getAbsolutePath(), true, new MixedMediaItemFactory());
	}

	@After
	public void after () {
		if (this.undertest != null) this.undertest.dispose();
	}

	@Test
	public void itCreatesDb () throws Exception {
		assertTrue(this.dbFile.exists());
	}

	@Test
	public void itSearchesForSingleItemByName () throws Exception {
		final String mediaNameFragment = "foo_bar_desu_" + System.currentTimeMillis();
		final File mediaFile = mockMediaFile(mediaNameFragment);
		this.undertest.addFile(MediaType.TRACK, mediaFile);

		final List<IMixedMediaItem> actual = this.undertest.simpleSearchMedia(MediaType.TRACK, MediaItemDb.escapeSearch(mediaNameFragment), MediaItemDb.SEARCH_ESC, 10);

		assertNotNull(actual);
		assertEquals(1, actual.size());
		final IMixedMediaItem item = actual.get(0);
		assertEquals(mediaFile.getAbsolutePath(), item.getFilepath());
	}

	private File mockMediaFile (final String nameFragment) throws IOException {
		return File.createTempFile("mock_media_" + nameFragment, "ext", this.tmp.getRoot());
	}

}
