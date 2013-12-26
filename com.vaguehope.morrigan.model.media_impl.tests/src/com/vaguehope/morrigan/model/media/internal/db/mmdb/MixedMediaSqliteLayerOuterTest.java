package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
import com.vaguehope.sqlitewrapper.DbException;

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
		final IMixedMediaItem expected = mockMediaFileWithNameContaining(mediaNameFragment);

		assertSingleResult(expected, runSearch(mediaNameFragment));
	}

	@Test
	public void itSearchesForSingleItemByTag () throws Exception {
		final String tag = "some_media_tag_" + System.nanoTime();
		final IMixedMediaItem expected = mockMediaFileWithTag(tag);

		assertSingleResult(expected, runSearch(tag));
	}

	@Test
	public void itSearchesForItemsByNameOrTag () throws Exception {
		final String term = "some_awesome_band_desu";
		final IMixedMediaItem expectedWithName = mockMediaFileWithNameContaining(term);
		final IMixedMediaItem expectedWithTag = mockMediaFileWithTag("watcha " + term + " noise");

		final List<IMixedMediaItem> actual = runSearch(term);

		assertEquals(2, actual.size());
		getItemByFilepath(actual, expectedWithName.getFilepath());
		getItemByFilepath(actual, expectedWithTag.getFilepath());
	}

	@Ignore("OR keyword not yet implemented")
	@Test
	public void itSearchesUsingMultipleTermsForItemsByNameOrTagUsingOrKeyword () throws Exception {
		final String term1 = "some_awesome_band_desu";
		final String term2 = "some_other_thing";
		final IMixedMediaItem expectedWithTerm1InName = mockMediaFileWithNameContaining(term1);
		final IMixedMediaItem expectedWithTerm2InName = mockMediaFileWithNameContaining(term2);
		final IMixedMediaItem expectedWithTerm1InTag = mockMediaFileWithTag("watcha " + term1 + " noise");
		final IMixedMediaItem expectedWithTerm2InTag = mockMediaFileWithTag("foo " + term2 + " bar");

		final List<IMixedMediaItem> actual = runSearch("  " + term1 + " OR " + term2 + " ");

		assertEquals(4, actual.size());
		getItemByFilepath(actual, expectedWithTerm1InName.getFilepath());
		getItemByFilepath(actual, expectedWithTerm2InName.getFilepath());
		getItemByFilepath(actual, expectedWithTerm1InTag.getFilepath());
		getItemByFilepath(actual, expectedWithTerm2InTag.getFilepath());
	}

	@Test
	public void itCanJustPartialMatchFileName () throws Exception {
		final String term = "some_awesome_band_desu";
		final IMixedMediaItem expectedWithTermInName = mockMediaFileWithNameContaining(term);
		mockMediaFileWithTag("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInName, runSearch("f~" + term));
	}

	@Test
	public void itCanJustPartialMatchTag () throws Exception {
		final String term = "some_awesome_band_desu";
		mockMediaFileWithNameContaining(term);
		final IMixedMediaItem expectedWithTermInTag = mockMediaFileWithTag("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInTag, runSearch("t~" + term));
	}

	@Test
	public void itCanJustExactlyMatchTag () throws Exception {
		final String term = "pl_desu";
		final IMixedMediaItem expectedWithTermAsTag = mockMediaFileWithTag(term);
		mockMediaFileWithTag("watcha " + term + " noise");

		assertSingleResult(expectedWithTermAsTag, runSearch("t=" + term));
	}

	@Ignore("Quotes not yet implemented")
	@Test
	public void itCanJustPartialMatchFileNameQuoted () throws Exception {
		final String term = "some awesome band desu";
		final IMixedMediaItem expectedWithTermInName = mockMediaFileWithNameContaining(term);
		mockMediaFileWithTag("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInName, runSearch("f~'" + term + "'"));
		assertSingleResult(expectedWithTermInName, runSearch("f~\"" + term + "\""));
	}

	private void addNoiseToDb () throws Exception {
		for (int i = 0; i < 10 ; i++) {
			mockMediaFileWithNameContaining("noise_" + i);
		}
		final List<IMixedMediaItem> all = this.undertest.getAllMedia(IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE, SortDirection.ASC, false);
		assertEquals(10, all.size());
	}

	private IMixedMediaItem mockMediaFileWithTag (final String tag) throws Exception {
		final IMixedMediaItem item = mockMediaFileWithNameContaining("tagged");
		this.undertest.addTag(item, tag, MediaTagType.MANUAL, (MediaTagClassification) null);
		return item;
	}

	private IMixedMediaItem mockMediaFileWithNameContaining (final String nameFragment) throws Exception {
		final File mediaFile = File.createTempFile("mock_media_" + nameFragment, ".ext", this.tmp.getRoot());
		this.undertest.addFile(MediaType.TRACK, mediaFile);
		final IMixedMediaItem item = this.undertest.getByFile(mediaFile);
		assertNotNull(item);
		assertEquals(mediaFile.getAbsolutePath(), item.getFilepath());
		this.undertest.addTag(item, "noise_tag_" + System.nanoTime(), MediaTagType.MANUAL, (MediaTagClassification) null);
		return item;
	}

	private List<IMixedMediaItem> runSearch (final String term) throws DbException {
		return this.undertest.simpleSearchMedia(MediaType.TRACK,
				MediaItemDb.escapeSearch(term),
				MediaItemDb.SEARCH_ESC, 10);
	}

	private static void assertSingleResult (final IMixedMediaItem expected, final List<IMixedMediaItem> actual) {
		assertEquals(1, actual.size());
		getItemByFilepath(actual, expected.getFilepath());
	}

	private static IMixedMediaItem getItemByFilepath (final List<IMixedMediaItem> list, final String filepath) {
		assertNotNull(list);
		for (IMixedMediaItem item : list) {
			if (filepath.equals(item.getFilepath())) return item;
		}
		throw new IllegalArgumentException("Filepath '" + filepath + "' not found in '" + list + "'.");
	}

}
