package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.model.db.IDbColumn;
import com.vaguehope.morrigan.model.media.IMediaItemStorageLayer.SortDirection;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IMixedMediaItemStorageLayer;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.sqlitewrapper.DbException;

public class MixedMediaSqliteLayerOuterTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	private List<IMixedMediaItem> expectedAllItems;

	private MixedMediaSqliteLayerOuter undertest;

	@Before
	public void before () throws Exception {
		this.undertest = new MixedMediaSqliteLayerOuter(":memory:", true, new MixedMediaItemFactory());
		this.expectedAllItems = new ArrayList<IMixedMediaItem>();
		addNoiseToDb();
	}

	@After
	public void after () {
		if (this.undertest != null) this.undertest.dispose();
	}

	@Test
	public void itReturnsAllItems () throws Exception {
		final List<IMixedMediaItem> actual = this.undertest.getAllMedia(
				new IDbColumn[] { IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE },
				new SortDirection[] { SortDirection.ASC },
				false);
		assertEquals(this.expectedAllItems, actual);
	}

	@Test
	public void itReturnsTrackItemsWhenDefaultTypeIsTrack () throws Exception {
		this.undertest.setDefaultMediaType(MediaType.TRACK);
		final List<IMixedMediaItem> actual = this.undertest.getMedia(
				new IDbColumn[] { IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE },
				new SortDirection[] { SortDirection.ASC },
				false);
		final List<IMixedMediaItem> expected = filterItemsByType(this.expectedAllItems, MediaType.TRACK);
		assertEquals(expected, actual);
	}

	@Test
	public void itReturnsPictureItemsWhenDefaultTypeIsPicture () throws Exception {
		this.undertest.setDefaultMediaType(MediaType.PICTURE);
		final List<IMixedMediaItem> actual = this.undertest.getMedia(
				new IDbColumn[] { IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE },
				new SortDirection[] { SortDirection.ASC },
				false);
		final List<IMixedMediaItem> expected = filterItemsByType(this.expectedAllItems, MediaType.PICTURE);
		assertEquals(expected, actual);
	}

	@Test
	public void itSearchesForSingleItemByName () throws Exception {
		final String mediaNameFragment = "some_media_file_" + System.nanoTime();
		final IMixedMediaItem expected = mockMediaTrackWithNameContaining(mediaNameFragment);

		assertSingleResult(expected, runSearch(mediaNameFragment));
	}

	@Test
	public void itSearchesForSingleItemByTag () throws Exception {
		final String tag = "some_media_tag_" + System.nanoTime();
		final IMixedMediaItem expected = mockMediaFileWithTags(tag);

		assertSingleResult(expected, runSearch(tag));
	}

	@Test
	public void itSearchesForSingleItemByNameAndTag () throws Exception {
		final String term1 = "foobar";
		final String term2 = "desu";
		mockMediaFileWithNameFragmentAndTags("watcha", term2);
		mockMediaFileWithNameFragmentAndTags(term1, "something");
		final IMixedMediaItem expected = mockMediaFileWithNameFragmentAndTags(term1, term2);

		assertSingleResult(expected, runSearch(term1 + " " + term2));
		assertSingleResult(expected, runSearch(term1 + "\t" + term2));
		assertSingleResult(expected, runSearch(term1 + "　" + term2)); // Ideographic Space.
	}

	@Test
	public void itSearchesForItemsByNameOrTag () throws Exception {
		final String term = "some_awesome_band_desu";
		final IMixedMediaItem expectedWithName = mockMediaTrackWithNameContaining(term);
		final IMixedMediaItem expectedWithTag = mockMediaFileWithTags("watcha " + term + " noise");

		final List<IMixedMediaItem> actual = runSearch(term);

		assertEquals(2, actual.size());
		getItemByFilepath(actual, expectedWithName.getFilepath());
		getItemByFilepath(actual, expectedWithTag.getFilepath());
	}

	@Test
	public void itSearchesWithoutCrashingWhenStartingWithOr () throws Exception {
		final String mediaNameFragment = "OR some_media_file_" + System.nanoTime();
		final IMixedMediaItem expected = mockMediaTrackWithNameContaining(mediaNameFragment);
		assertSingleResult(expected, runSearch(mediaNameFragment));
	}

	@Test
	public void itSearchesWithoutCrashingWhenEndingWithOr () throws Exception {
		final String mediaNameFragment = "some_media_file_" + System.nanoTime();
		final IMixedMediaItem expected = mockMediaTrackWithNameContaining(mediaNameFragment);
		assertSingleResult(expected, runSearch(mediaNameFragment + " OR"));
	}

	@Test
	public void itSearchesUsingMultipleTermsForItemsByNameOrTagUsingOrKeyword () throws Exception {
		final String term1 = "some_awesome_band_desu";
		final String term2 = "some_other_thing";
		final IMixedMediaItem expectedWithTerm1InName = mockMediaTrackWithNameContaining(term1);
		final IMixedMediaItem expectedWithTerm2InName = mockMediaTrackWithNameContaining(term2);
		final IMixedMediaItem expectedWithTerm1InTag = mockMediaFileWithTags("watcha " + term1 + " noise");
		final IMixedMediaItem expectedWithTerm2InTag = mockMediaFileWithTags("foo " + term2 + " bar");

		final List<IMixedMediaItem> actual = runSearch("  " + term1 + " OR " + term2 + " ");

		assertEquals(4, actual.size());
		getItemByFilepath(actual, expectedWithTerm1InName.getFilepath());
		getItemByFilepath(actual, expectedWithTerm2InName.getFilepath());
		getItemByFilepath(actual, expectedWithTerm1InTag.getFilepath());
		getItemByFilepath(actual, expectedWithTerm2InTag.getFilepath());
	}

	@Test
	public void itSearchesForJustPartialMatchFileName () throws Exception {
		final String term = "some_awesome_band_desu";
		final IMixedMediaItem expectedWithTermInName = mockMediaTrackWithNameContaining(term);
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInName, runSearch("f~" + term));
	}

	@Test
	public void itSearchesForJustPartialMatchFileNameUcaseType () throws Exception {
		final String term = "some_awesome_band_desu";
		final IMixedMediaItem expectedWithTermInName = mockMediaTrackWithNameContaining(term);
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInName, runSearch("F~" + term));
	}

	@Test
	public void itSearchesForMatchFileNameWithSpecialChars () throws Exception {
		final String term = "awesome'\"*%_\\band";
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(mockMediaTrackWithNameContaining(term), runSearch("f~" + term));
	}

	@Test
	public void itSearchesForJustPartialMatchFileNameEndAnchored () throws Exception {
		final IMixedMediaItem expectedWithTermInName = mockMediaTrackWithNameContaining("/foo/thing/bip bop bar", ".myext");
		mockMediaFileWithTags("watcha foo.myext noise");

		assertSingleResult(expectedWithTermInName, runSearch("f~.myext$"));
	}

	@Test
	public void itSearchesForJustPartialMatchTag () throws Exception {
		final String term = "some_awesome_band_desu";
		mockMediaTrackWithNameContaining(term);
		final IMixedMediaItem expectedWithTermInTag = mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInTag, runSearch("t~" + term));
	}

	@Test
	public void itSearchesForJustPartialMatchTagUcaseType () throws Exception {
		final String term = "some_awesome_band_desu";
		mockMediaTrackWithNameContaining(term);
		final IMixedMediaItem expectedWithTermInTag = mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInTag, runSearch("T~" + term));
	}

	@Test
	public void itSearchesForJustPartialMatchTagStartAnchored () throws Exception {
		final String term = "some_awesome_band_desu";
		mockMediaTrackWithNameContaining(term);
		final IMixedMediaItem expectedWithTermInTag = mockMediaFileWithTags(term + " watcha");
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInTag, runSearch("t~^" + term));
	}

	@Test
	public void itSearchesForJustPartialMatchTagEndAnchored () throws Exception {
		final String term = "some_awesome_band_desu";
		mockMediaTrackWithNameContaining(term);
		final IMixedMediaItem expectedWithTermInTag = mockMediaFileWithTags("watcha " + term);
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInTag, runSearch("t~" + term + "$"));
	}

	@Test
	public void itSearchesForJustExactlyMatchTag () throws Exception {
		final String term = "pl_desu";
		final IMixedMediaItem expectedWithTermAsTag = mockMediaFileWithTags(term);
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermAsTag, runSearch("t=" + term));
	}

	@Test
	public void itSearchesForJustExactlyMatchTagUcaseType () throws Exception {
		final String term = "pl_desu";
		final IMixedMediaItem expectedWithTermAsTag = mockMediaFileWithTags(term);
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermAsTag, runSearch("T=" + term));
	}

	@Test
	public void itSearchesForSingleItemByNameAndTagSpecifically () throws Exception {
		final String term1 = "foobar";
		final String term2 = "desu";
		mockMediaFileWithNameFragmentAndTags("watcha", term2);
		mockMediaFileWithNameFragmentAndTags(term1, "something");
		final IMixedMediaItem expected = mockMediaFileWithNameFragmentAndTags(term1, term2);

		assertSingleResult(expected, runSearch("f~" + term1 + " t=" + term2));
	}

	@Test
	public void itSearchesForItemsThatMatchTwoTags () throws Exception {
		final IMixedMediaItem expectedWithTags = mockMediaFileWithTags(
				"some_awesome_band_desu",
				"happy_track_nyan~"
				);

		final List<IMixedMediaItem> actual = runSearch("some_awesome_band_desu happy_track_nyan~");

		assertSingleResult(expectedWithTags, actual);
	}

	@Test
	public void itSearchesForItemsThatMatchTwoExplicitTags () throws Exception {
		final IMixedMediaItem expectedWithTags = mockMediaFileWithTags(
				"some_awesome_band_desu",
				"happy_track_nyan~");

		final List<IMixedMediaItem> actual = runSearch("t=some_awesome_band_desu t=happy_track_nyan~");
		assertSingleResult(expectedWithTags, actual);
	}

	/**
	 * Example to prove Saved Search does same as simpleSearch().
	 * TODO add other cases to be more sure?
	 */
	@Test
	public void itSavedSearchesForItemsThatMatchTwoExplicitTags () throws Exception {
		final IMixedMediaItem expectedWithTags = mockMediaFileWithTags(
				"some_awesome_band_desu",
				"happy_track_nyan~");

		final List<IMixedMediaItem> actual = this.undertest.getMedia(
				new IDbColumn[] { IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE },
				new SortDirection[] { SortDirection.ASC },
				true,
				"t=some_awesome_band_desu t=happy_track_nyan~");
		assertSingleResult(expectedWithTags, actual);
	}

	@Test
	public void itSearchesForPartialMatchFileNameQuoted () throws Exception {
		final String term = "some awesome? band desu";

		final String term1 = term.replace('?', '"');
		assertSingleResult(mockMediaTrackWithNameContaining(term1), runSearch("'" + term1 + "'"));

		final String term2 = term.replace('?', '\'');
		assertSingleResult(mockMediaTrackWithNameContaining(term2), runSearch("\"" + term2 + "\""));
	}

	@Test
	public void itSearchesForJustPartialMatchFileNameQuoted () throws Exception {
		final String term = "some awesome? band desu";
		mockMediaFileWithTags("watcha " + term + " noise");

		final String term1 = term.replace('?', '"');
		assertSingleResult(mockMediaTrackWithNameContaining(term1), runSearch("f~'" + term1 + "'"));

		final String term2 = term.replace('?', '\'');
		assertSingleResult(mockMediaTrackWithNameContaining(term2), runSearch("f~\"" + term2 + "\""));
	}

	private void addNoiseToDb () throws Exception {
		for (int i = 0; i < 10; i++) {
			mockMediaFileWithNameContaining(
					i % 2 == 0 ? MediaType.TRACK : MediaType.PICTURE,
					"noise_" + i);
		}
		final List<IMixedMediaItem> all = this.undertest.getAllMedia(
				new IDbColumn[] { IMixedMediaItemStorageLayer.SQL_TBL_MEDIAFILES_COL_FILE },
				new SortDirection[] { SortDirection.ASC },
				false);
		assertEquals(10, all.size());
	}

	private IMixedMediaItem mockMediaFileWithTags (final String... tags) throws Exception {
		return mockMediaFileWithNameFragmentAndTags("tagged", tags);
	}

	private IMixedMediaItem mockMediaFileWithNameFragmentAndTags (final String nameFragment, final String... tags) throws Exception {
		final IMixedMediaItem item = mockMediaTrackWithNameContaining(nameFragment);
		for (final String tag : tags) {
			this.undertest.addTag(item, tag, MediaTagType.MANUAL, (MediaTagClassification) null);
		}
		return item;
	}

	private IMixedMediaItem mockMediaTrackWithNameContaining (final String nameFragment) throws Exception {
		return mockMediaFileWithNameContaining(MediaType.TRACK, nameFragment);
	}

	private IMixedMediaItem mockMediaTrackWithNameContaining (final String nameFragment, final String nameSuffex) throws Exception {
		return mockMediaFileWithName(MediaType.TRACK, nameFragment, nameSuffex);
	}

	private IMixedMediaItem mockMediaFileWithNameContaining (final MediaType type, final String nameFragment) throws Exception {
		return mockMediaFileWithName(type, nameFragment, ".ext");
	}

	private IMixedMediaItem mockMediaFileWithName (final MediaType type, final String nameFragment, final String nameSuffex) throws Exception {
		final File mediaFile = File.createTempFile("mock_media_" + nameFragment, nameSuffex, this.tmp.getRoot());
		this.undertest.addFile(type, mediaFile);
		final IMixedMediaItem item = this.undertest.getByFile(mediaFile);
		assertNotNull(item);
		assertEquals(mediaFile.getAbsolutePath(), item.getFilepath());
		this.undertest.addTag(item, "noise_tag_" + System.nanoTime(), MediaTagType.MANUAL, (MediaTagClassification) null);
		this.expectedAllItems.add(item);
		return item;
	}

	private List<IMixedMediaItem> runSearch (final String term) throws DbException {
		return this.undertest.simpleSearchMedia(MediaType.TRACK, term, 10);
	}

	private static void assertSingleResult (final IMixedMediaItem expected, final List<IMixedMediaItem> actual) {
		assertEquals(1, actual.size());
		getItemByFilepath(actual, expected.getFilepath());
	}

	private static IMixedMediaItem getItemByFilepath (final List<IMixedMediaItem> list, final String filepath) {
		assertNotNull(list);
		for (final IMixedMediaItem item : list) {
			if (filepath.equals(item.getFilepath())) return item;
		}
		throw new IllegalArgumentException("Filepath '" + filepath + "' not found in '" + list + "'.");
	}

	private static List<IMixedMediaItem> filterItemsByType (final List<IMixedMediaItem> list, final MediaType type) {
		final ArrayList<IMixedMediaItem> ret = new ArrayList<IMixedMediaItem>();
		for (final IMixedMediaItem item : list) {
			if (item.getMediaType() == type) ret.add(item);
		}
		if (ret.size() < 1) throw new IllegalStateException("Filter excluded all items.");
		return ret;
	}

}
