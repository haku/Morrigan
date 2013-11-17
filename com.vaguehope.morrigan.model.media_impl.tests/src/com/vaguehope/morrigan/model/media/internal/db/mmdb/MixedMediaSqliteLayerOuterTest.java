package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMixedMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.internal.db.MediaItemDb;

public class MixedMediaSqliteLayerOuterTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	private File dbFile;

	private MixedMediaSqliteLayerOuter undertest;

	@Before
	public void before () throws Exception {
		this.dbFile = this.tmp.newFile("testdb.db3");
		this.undertest = new MixedMediaSqliteLayerOuter(this.dbFile.getAbsolutePath(), true, new MixedMediaItemFactory());
		addNoiseToDb();
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
		final String mediaNameFragment = "some_media_file_" + System.nanoTime();
		final IMixedMediaItem item = mockMediaFile(mediaNameFragment);

		final List<IMixedMediaItem> actual = this.undertest.simpleSearchMedia(MediaType.TRACK, MediaItemDb.escapeSearch(mediaNameFragment), MediaItemDb.SEARCH_ESC, 10);

		final IMixedMediaItem actualItem = getSingleItem(actual);
		assertEquals(item.getFilepath(), actualItem.getFilepath());
	}

	@Test
	public void itSearchesForSingleItemByTag () throws Exception {
		final IMixedMediaItem item = mockMediaFile();
		final String tag = "some_media_tag_" + System.nanoTime();
		this.undertest.addTag(item, tag, MediaTagType.MANUAL, (MediaTagClassification) null);

		final List<IMixedMediaItem> actual = this.undertest.simpleSearchMedia(MediaType.TRACK, MediaItemDb.escapeSearch(tag), MediaItemDb.SEARCH_ESC, 10);

		final IMixedMediaItem actualItem = getSingleItem(actual);
		assertEquals(item.getFilepath(), actualItem.getFilepath());
	}

	private void addNoiseToDb () throws Exception {
		for (int i = 0; i < 10 ; i++) {
			mockMediaFile("noise_" + i + "_" + System.nanoTime());
		}
		final List<IMixedMediaItem> all = this.undertest.getAllMedia(IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE, SortDirection.ASC, false);
		assertEquals(10, all.size());
	}

	private IMixedMediaItem mockMediaFile () throws Exception {
		return mockMediaFile("target_" + System.nanoTime());
	}

	private IMixedMediaItem mockMediaFile (final String nameFragment) throws Exception {
		final File mediaFile = File.createTempFile("mock_media_" + nameFragment, "ext", this.tmp.getRoot());
		this.undertest.addFile(MediaType.TRACK, mediaFile);
		final IMixedMediaItem item = this.undertest.getByFile(mediaFile);
		assertNotNull(item);
		assertEquals(mediaFile.getAbsolutePath(), item.getFilepath());
		return item;
	}

	private static <T> T getSingleItem (final List<T> list) {
		assertNotNull(list);
		assertEquals(1, list.size());
		final T item = list.get(0);
		assertNotNull(item);
		return item;
	}

}
