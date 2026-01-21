package morrigan.model.media.internal.db;

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

import morrigan.model.media.MediaItem;
import morrigan.model.media.SortColumn;
import morrigan.model.media.MediaItem.MediaType;
import morrigan.model.media.SortColumn.SortDirection;
import morrigan.model.media.internal.db.DefaultMediaItemFactory;
import morrigan.model.media.internal.db.MediaSqliteLayer;
import morrigan.sqlitewrapper.DbException;

public class MediaSqliteLayerTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	private List<MediaItem> expectedAllItems;

	private MediaSqliteLayer undertest;

	@Before
	public void before () throws Exception {
		this.undertest = new MediaSqliteLayer(":memory:", true, new DefaultMediaItemFactory(null));
		this.expectedAllItems = new ArrayList<>();
		addNoiseToDb();
	}

	@After
	public void after () {
		if (this.undertest != null) this.undertest.dispose();
	}

	@Test
	public void itReturnsAllItems () throws Exception {
		final List<MediaItem> actual = this.undertest.getAllMedia(
				new SortColumn[] { SortColumn.FILE_PATH },
				new SortDirection[] { SortDirection.ASC },
				false);
		assertEquals(this.expectedAllItems, actual);
	}

	@Test
	public void itReturnsTrackItemsWhenTypeIsTrack () throws Exception {
		final List<MediaItem> actual = this.undertest.getMedia(
				MediaType.TRACK,
				new SortColumn[] { SortColumn.FILE_PATH },
				new SortDirection[] { SortDirection.ASC },
				false);
		final List<MediaItem> expected = filterItemsByType(this.expectedAllItems, MediaType.TRACK);
		assertEquals(expected, actual);
	}

	@Test
	public void itReturnsPictureItemsWhenTypeIsPicture () throws Exception {
		final List<MediaItem> actual = this.undertest.getMedia(
				MediaType.PICTURE,
				new SortColumn[] { SortColumn.FILE_PATH },
				new SortDirection[] { SortDirection.ASC },
				false);
		final List<MediaItem> expected = filterItemsByType(this.expectedAllItems, MediaType.PICTURE);
		assertEquals(expected, actual);
	}

	@Test
	public void itSearchesForSingleItemByName () throws Exception {
		final String mediaNameFragment = "some_media_file_" + System.nanoTime();
		final MediaItem expected = mockMediaTrackWithNameContaining(mediaNameFragment);

		assertSingleResult(expected, runSearch(mediaNameFragment));
	}

	@Test
	public void itSearchesForSingleItemByTag () throws Exception {
		final String tag = "some_media_tag_" + System.nanoTime();
		final MediaItem expected = mockMediaFileWithTags(tag);

		assertSingleResult(expected, runSearch(tag));
	}

	@Test
	public void itSearchesForSingleItemByNameAndTag () throws Exception {
		final String term1 = "foobar";
		final String term2 = "desu";
		mockMediaFileWithNameFragmentAndTags("watcha", term2);
		mockMediaFileWithNameFragmentAndTags(term1, "something");
		final MediaItem expected = mockMediaFileWithNameFragmentAndTags(term1, term2);

		assertSingleResult(expected, runSearch(term1 + " " + term2));
		assertSingleResult(expected, runSearch(term1 + "\t" + term2));
		assertSingleResult(expected, runSearch(term1 + "　" + term2)); // Ideographic Space.
	}

	@Test
	public void itSearchesForItemsByNameOrTag () throws Exception {
		final String term = "some_awesome_band_desu";
		final MediaItem expectedWithName = mockMediaTrackWithNameContaining(term);
		final MediaItem expectedWithTag = mockMediaFileWithTags("watcha " + term + " noise");

		final List<MediaItem> actual = runSearch(term);

		assertEquals(2, actual.size());
		getItemByFilepath(actual, expectedWithName.getFilepath());
		getItemByFilepath(actual, expectedWithTag.getFilepath());
	}

	@Test
	public void itSearchesWithoutCrashingWhenStartingWithOr () throws Exception {
		final String mediaNameFragment = "OR some_media_file_" + System.nanoTime();
		final MediaItem expected = mockMediaTrackWithNameContaining(mediaNameFragment);
		assertSingleResult(expected, runSearch(mediaNameFragment));
	}

	@Test
	public void itSearchesWithoutCrashingWhenEndingWithOr () throws Exception {
		final String mediaNameFragment = "some_media_file_" + System.nanoTime();
		final MediaItem expected = mockMediaTrackWithNameContaining(mediaNameFragment);
		assertSingleResult(expected, runSearch(mediaNameFragment + " OR"));
	}

	@Test
	public void itSearchesUsingMultipleTermsForItemsByNameOrTagUsingOrKeyword () throws Exception {
		final String term1 = "some_awesome_band_desu";
		final String term2 = "some_other_thing";
		final MediaItem expectedWithTerm1InName = mockMediaTrackWithNameContaining(term1);
		final MediaItem expectedWithTerm2InName = mockMediaTrackWithNameContaining(term2);
		final MediaItem expectedWithTerm1InTag = mockMediaFileWithTags("watcha " + term1 + " noise");
		final MediaItem expectedWithTerm2InTag = mockMediaFileWithTags("foo " + term2 + " bar");

		final String[] queries = new String[] {
				"  " + term1 + " OR " + term2 + " ",
				"  " + term1 + " OR OR " + term2 + " ",
		};
		for (final String q : queries) {
			final List<MediaItem> actual = runSearch(q);
			assertEquals(4, actual.size());
			getItemByFilepath(actual, expectedWithTerm1InName.getFilepath());
			getItemByFilepath(actual, expectedWithTerm2InName.getFilepath());
			getItemByFilepath(actual, expectedWithTerm1InTag.getFilepath());
			getItemByFilepath(actual, expectedWithTerm2InTag.getFilepath());
		}
	}

	@Test
	public void itSearchesForJustPartialMatchFileName () throws Exception {
		final String term = "some_awesome_band_desu";
		final MediaItem expectedWithTermInName = mockMediaTrackWithNameContaining(term);
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInName, runSearch("f~" + term));
	}

	@Test
	public void itSearchesForJustPartialMatchFileNameUcaseType () throws Exception {
		final String term = "some_awesome_band_desu";
		final MediaItem expectedWithTermInName = mockMediaTrackWithNameContaining(term);
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInName, runSearch("F~" + term));
	}

	@Test
	public void itSearchesForMatchFileNameWithSpecialChars () throws Exception {
		final String term = "awesome'\"*%_\\)(band";

		// Noise:
		mockMediaFileWithTags("watcha " + term + " noise");

		final MediaItem expected = mockMediaTrackWithNameContaining(term);
		assertSingleResult(expected, runSearch("f~" + term));
	}

	@Test
	public void itSearchesForMatchTagWithSpecialChars () throws Exception {
		final String tag = "awesome'\"*%_\\)(band";

		// Noise:
		mockMediaFileWithTags("watcha " + tag + " noise");
		mockMediaFileWithTags("watcha awesome'\"*%_\\", ")(band noise");
		mockMediaFileWithTags("watcha awesome'\"*%_\\)", "(band noise");
		mockMediaFileWithTags("watcha awesome'\"*%_\\)(", "band noise");
		mockMediaFileWithTags("watcha awesome", "'\"*%_\\)(band noise");
		mockMediaFileWithTags("watcha awesome'", "\"*%_\\)(band noise");
		mockMediaFileWithTags("watcha awesome'\"", "*%_\\)(band noise");

		final MediaItem expected = mockMediaFileWithTags(tag);
		assertSingleResult(expected, runSearch("t=" + tag));
	}

	@Test
	public void itSearchesForJustPartialMatchFileNameEndAnchored () throws Exception {
		final MediaItem expectedWithTermInName = mockMediaTrackWithNameContaining("/foo/thing/bip bop bar", ".myext");
		mockMediaFileWithTags("watcha foo.myext noise");

		assertSingleResult(expectedWithTermInName, runSearch("f~.myext$"));
	}

	@Test
	public void itSearchesForJustPartialMatchTag () throws Exception {
		final String term = "some_awesome_band_desu";
		mockMediaTrackWithNameContaining(term);
		final MediaItem expectedWithTermInTag = mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInTag, runSearch("t~" + term));
	}

	@Test
	public void itSearchesForJustPartialMatchTagUcaseType () throws Exception {
		final String term = "some_awesome_band_desu";
		mockMediaTrackWithNameContaining(term);
		final MediaItem expectedWithTermInTag = mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInTag, runSearch("T~" + term));
	}

	@Test
	public void itSearchesForJustPartialMatchTagStartAnchored () throws Exception {
		final String term = "some_awesome_band_desu";
		mockMediaTrackWithNameContaining(term);
		final MediaItem expectedWithTermInTag = mockMediaFileWithTags(term + " watcha");
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInTag, runSearch("t~^" + term));
	}

	@Test
	public void itSearchesForJustPartialMatchTagEndAnchored () throws Exception {
		final String term = "some_awesome_band_desu";
		mockMediaTrackWithNameContaining(term);
		final MediaItem expectedWithTermInTag = mockMediaFileWithTags("watcha " + term);
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermInTag, runSearch("t~" + term + "$"));
	}

	@Test
	public void itSearchesForJustExactlyMatchTag () throws Exception {
		final String term = "pl_desu";
		final MediaItem expectedWithTermAsTag = mockMediaFileWithTags(term);
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermAsTag, runSearch("t=" + term));
	}

	@Test
	public void itSearchesForJustExactlyMatchTagUcaseType () throws Exception {
		final String term = "pl_desu";
		final MediaItem expectedWithTermAsTag = mockMediaFileWithTags(term);
		mockMediaFileWithTags("watcha " + term + " noise");

		assertSingleResult(expectedWithTermAsTag, runSearch("T=" + term));
	}

	@Test
	public void itSearchesForSingleItemByNameAndTagSpecifically () throws Exception {
		final String term1 = "foobar";
		final String term2 = "desu";
		mockMediaFileWithNameFragmentAndTags("watcha", term2);
		mockMediaFileWithNameFragmentAndTags(term1, "something");
		final MediaItem expected = mockMediaFileWithNameFragmentAndTags(term1, term2);

		assertSingleResult(expected, runSearch("f~" + term1 + " t=" + term2));
	}

	@Test
	public void itSearchesForItemsThatMatchTwoTags () throws Exception {
		final MediaItem expectedWithTags = mockMediaFileWithTags(
				"some_awesome_band_desu",
				"happy_track_nyan~"
				);

		final List<MediaItem> actual = runSearch("some_awesome_band_desu happy_track_nyan~");

		assertSingleResult(expectedWithTags, actual);
	}

	@Test
	public void itSearchesForItemsThatMatchTwoExplicitTags () throws Exception {
		final MediaItem expectedWithTags = mockMediaFileWithTags(
				"some_awesome_band_desu",
				"happy_track_nyan~");

		final List<MediaItem> actual = runSearch("t=some_awesome_band_desu t=happy_track_nyan~");
		assertSingleResult(expectedWithTags, actual);
	}

	/**
	 * Example to prove Saved Search does same as search().
	 * TODO add other cases to be more sure?
	 */
	@Test
	public void itSavedSearchesForItemsThatMatchTwoExplicitTags () throws Exception {
		final MediaItem expectedWithTags = mockMediaFileWithTags(
				"some_awesome_band_desu",
				"happy_track_nyan~");

		final List<MediaItem> actual = this.undertest.getMedia(
				MediaType.TRACK,
				new SortColumn[] { SortColumn.FILE_PATH },
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

	@Test
	public void itSearchesPathWithEscapedSingleQuotes () throws Exception {
		final String path = "some \"awesome' band\" desu";
		final String search = "f~'some \"awesome\\' band\" desu'";

		final MediaItem expected = mockMediaTrackWithNameContaining(path);
		assertSingleResult(expected, runSearch(search));
	}

	@Test
	public void itSearchesPathWithEscapedDoubleQuotes () throws Exception {
		final String path = "some 'awesome\" band' desu";
		final String search = "f~\"some 'awesome\\\" band' desu\"";

		final MediaItem expected = mockMediaTrackWithNameContaining(path);
		assertSingleResult(expected, runSearch(search));
	}

	@Test
	public void itSearchesTagWithEscapedSingleQuote () throws Exception {
		final String tag = "some media' tag ";
		final String search = "t=some' media\\' tag '";

		final MediaItem expected = mockMediaFileWithTags(tag);
		assertSingleResult(expected, runSearch(search));
	}

	@Test
	public void itSearchesTagWithEscapedSingleQuote2 () throws Exception {
		final String tag = "some \\\"media' tag ";
		final String search = "t=some' \\\"media\\' tag '";

		final MediaItem expected = mockMediaFileWithTags(tag);
		assertSingleResult(expected, runSearch(search));
	}

	@Test
	public void itSearchesTagWithEscapedDoubleQuote () throws Exception {
		final String tag = "some media\" tag ";
		final String search = "t=some\" media\\\" tag \"";

		final MediaItem expected = mockMediaFileWithTags(tag);
		assertSingleResult(expected, runSearch(search));
	}

	@Test
	public void itSearchesTagWithEscapedDoubleQuote2 () throws Exception {
		final String tag = "some \\'media\" tag ";
		final String search = "t=some\" \\'media\\\" tag \"";

		final MediaItem expected = mockMediaFileWithTags(tag);
		assertSingleResult(expected, runSearch(search));
	}

	@Test
	public void itSearchesUsingMultipleTermsAndBrackets () throws Exception {
		final MediaItem expected1 = mockMediaFileWithNameFragmentAndTags("some_folder", "foo");
		final MediaItem expected2 = mockMediaFileWithNameFragmentAndTags("some_folder", "bar");

		// Noise:
		mockMediaFileWithNameFragmentAndTags("other_folder", "foo");
		mockMediaFileWithNameFragmentAndTags("other_folder", "bar");

		// Verify:
		assertSingleResult(expected1, runSearch("t=foo f~some_folder"));
		assertSingleResult(expected2, runSearch("t=bar f~some_folder"));

		final String[] queries = new String[] {
				"(t=bar OR t=foo) f~some_folder",
				"f~some_folder (t=bar OR t=foo)",
				"f~some_folder AND (t=bar OR t=foo)",
				"f~some_folder AND AND (t=bar OR t=foo)",
		};
		for (final String q : queries) {
			final List<MediaItem> actual = runSearch(q);
			assertEquals(2, actual.size());
			getItemByFilepath(actual, expected1.getFilepath());
			getItemByFilepath(actual, expected2.getFilepath());
		}
	}

	@Test
	public void itSearchesWithoutCrashingWithUnbalancedOpenBracket () throws Exception {
		final String mediaNameFragment = "some_media_file_" + System.nanoTime();
		final MediaItem expected = mockMediaTrackWithNameContaining(mediaNameFragment);
		assertSingleResult(expected, runSearch("(" + mediaNameFragment));
	}

	@Test
	public void itSearchesWithoutCrashingWithUnbalancedOpenBracketAndLeadingOr () throws Exception {
		final String mediaNameFragment = "some_media_file_" + System.nanoTime();
		final MediaItem expected = mockMediaTrackWithNameContaining(mediaNameFragment);
		assertSingleResult(expected, runSearch("( OR " + mediaNameFragment));
	}

	@Test
	public void itSearchesWithoutCrashingWithUnbalancedCloseBracket () throws Exception {
		final String mediaNameFragment = "some_media_file_" + System.nanoTime();
		final MediaItem expected = mockMediaTrackWithNameContaining(mediaNameFragment);
		assertSingleResult(expected, runSearch(mediaNameFragment + ")"));
	}

	@Test
	public void itSearchesWithoutCrashingWithUnbalancedCloseBracketAndTrailingOr () throws Exception {
		final String mediaNameFragment = "some_media_file_" + System.nanoTime();
		final MediaItem expected = mockMediaTrackWithNameContaining(mediaNameFragment);
		assertSingleResult(expected, runSearch(mediaNameFragment + " OR )"));
	}

	@Test
	public void itSearchesForNegativeTagExactMatch () throws Exception {
		mockMediaFileWithTags("abc", "foobar");
		final MediaItem nonMatch = mockMediaFileWithTags("abc", "desu");
		assertSingleResult(nonMatch, runSearch("t=abc -t=foobar"));
	}

	@Test
	public void itSearchesForNegativeTagPartialMatch () throws Exception {
		mockMediaFileWithTags("abc", "foobar");
		final MediaItem nonMatch = mockMediaFileWithTags("abc", "desu");
		assertSingleResult(nonMatch, runSearch("t=abc -t~foo"));
	}

	@Test
	public void itSearchesForNegativeFilePartialMatch () throws Exception {
		mockMediaFileWithNameFragmentAndTags("foobar", "abc");
		final MediaItem nonMatch = mockMediaFileWithTags("abc");
		assertSingleResult(nonMatch, runSearch("t=abc -f~foo"));
	}

	@Test
	public void itDoesNotCrashWithBadQuery() throws Exception {
		runSearch("t=\"a very long tag that does not fit on the screen properl");
	}

	private void addNoiseToDb () throws Exception {
		for (int i = 0; i < 10; i++) {
			mockMediaFileWithNameContaining(
					i % 2 == 0 ? MediaType.TRACK : MediaType.PICTURE,
					"noise_" + i);
		}
		final List<MediaItem> all = this.undertest.getAllMedia(
				new SortColumn[] { SortColumn.FILE_PATH },
				new SortDirection[] { SortDirection.ASC },
				false);
		assertEquals(10, all.size());
	}

	private MediaItem mockMediaFileWithTags (final String... tags) throws Exception {
		return mockMediaFileWithNameFragmentAndTags("tagged", tags);
	}

	private MediaItem mockMediaFileWithNameFragmentAndTags (final String nameFragment, final String... tags) throws Exception {
		final MediaItem item = mockMediaTrackWithNameContaining(nameFragment);
		for (final String tag : tags) {
			this.undertest.addTag(item, tag);
		}
		return item;
	}

	private MediaItem mockMediaTrackWithNameContaining (final String nameFragment) throws Exception {
		return mockMediaFileWithNameContaining(MediaType.TRACK, nameFragment);
	}

	private MediaItem mockMediaTrackWithNameContaining (final String nameFragment, final String nameSuffex) throws Exception {
		return mockMediaFileWithName(MediaType.TRACK, nameFragment, nameSuffex);
	}

	private MediaItem mockMediaFileWithNameContaining (final MediaType type, final String nameFragment) throws Exception {
		return mockMediaFileWithName(type, nameFragment, ".ext");
	}

	private MediaItem mockMediaFileWithName (final MediaType type, final String nameFragment, final String nameSuffex) throws Exception {
		final File mediaFile = File.createTempFile("mock_media_" + nameFragment, nameSuffex, this.tmp.getRoot());
		this.undertest.addFile(type, mediaFile);
		final MediaItem item = this.undertest.getByFile(mediaFile);
		assertNotNull(item);
		assertEquals(mediaFile.getAbsolutePath(), item.getFilepath());
		this.undertest.addTag(item, "noise_tag_" + System.nanoTime());
		this.expectedAllItems.add(item);
		return item;
	}

	private List<MediaItem> runSearch (final String term) throws DbException {
		return this.undertest.search(MediaType.TRACK, term, 10, null, null, false);
	}

	private static void assertSingleResult (final MediaItem expected, final List<MediaItem> actual) {
		assertEquals(String.format("expected=%s actual=%s", expected, actual), 1, actual.size());
		getItemByFilepath(actual, expected.getFilepath());
	}

	private static MediaItem getItemByFilepath (final List<MediaItem> list, final String filepath) {
		assertNotNull(list);
		for (final MediaItem item : list) {
			if (filepath.equals(item.getFilepath())) return item;
		}
		throw new IllegalArgumentException("Filepath '" + filepath + "' not found in '" + list + "'.");
	}

	private static List<MediaItem> filterItemsByType (final List<MediaItem> list, final MediaType type) {
		final ArrayList<MediaItem> ret = new ArrayList<>();
		for (final MediaItem item : list) {
			if (item.getMediaType() == type) ret.add(item);
		}
		if (ret.size() < 1) throw new IllegalStateException("Filter excluded all items.");
		return ret;
	}

}
