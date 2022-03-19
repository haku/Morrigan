package com.vaguehope.morrigan.sshui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.vaguehope.morrigan.sshui.Autocomplete.MergedResult;
import com.vaguehope.morrigan.sshui.Autocomplete.PartialQuery;

public class AutocompleteTest {

	@Test
	public void itExtractsTermBeingTyped() throws Exception {
		testExtract("", 0);
		testExtract("=", 1);
		testExtract("~", 1);
		testExtract("at=", 3);
		testExtract("a t=", 4, "t=", 2);

		testExtract("t=", 2, "t=", 0);
		testExtract("t= ", 3);
		testExtract("t= abc", 2, "t=", 0);
		testExtract("t= abc", 3);

		testExtract("t=abc", 5, "t=abc", 0);
		testExtract("t=abc ", 6);
		testExtract("t=abc t", 7);
		testExtract("t=abc t=", 8, "t=", 6);
		testExtract("t=abc t=123", 11, "t=123", 6);

		testExtract("t~", 2, "t~", 0);
		testExtract("t~a", 3, "t~a", 0);
		testExtract("t~ abc", 2, "t~", 0);
	}

	private static void testExtract(final String text, final int caretPos) {
		assertEquals(new PartialQuery(text, null, -1), Autocomplete.extractPartialSearchTerm(text, caretPos));
	}

	private static void testExtract(final String text, final int caretPos, final String term, final int termStart) {
		assertEquals(new PartialQuery(text, term, termStart), Autocomplete.extractPartialSearchTerm(text, caretPos));
	}

	@Test
	public void itMergesResult() throws Exception {
		testMerge("t=", "t=", 0, "abc", "t=abc", 5);
		testMerge("t=a", "t=a", 0, "abc", "t=abc", 5);
		testMerge("t=a t=b", "t=a", 0, "abc", "t=abc t=b", 5);
		testMerge("t=a t=b", "t=b", 4, "bcd", "t=a t=bcd", 9);

		testMerge("t=a", "t=a", 0, "a b c d", "t=\"a b c d\"", 11);
		testMerge("t=a", "t=a", 0, "a'b c d", "t=\"a'b c d\"", 11);
		testMerge("t=a", "t=a", 0, "a b\"c d", "t=\'a b\"c d'", 11);
		testMerge("t=a", "t=a", 0, "a'b\"c d", "t=\'a\\'b\"c d'", 12);
		testMerge("t=a", "t=a", 0, "a'b\"c", "t=\'a\\'b\"c'", 10);
	}

	private static void testMerge(final String text, final String term, final int termStart, final String result, final String expectedText, final int carotPos) {
		assertEquals(new MergedResult(expectedText, carotPos), Autocomplete.mergeResult(text, new PartialQuery(text, term, termStart), result));
	}

}
