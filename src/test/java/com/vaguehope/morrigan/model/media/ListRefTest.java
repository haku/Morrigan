package com.vaguehope.morrigan.model.media;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import com.vaguehope.morrigan.model.media.ListRef.ListType;

public class ListRefTest {

	@Test
	public void itDoesAll4Parts() throws Exception {
		testRoundTrip("LOCAL:l=li%3Ast%2FId&n=node.Id&s=a%3Db", new ListRef(ListType.LOCAL, "li:st/Id", "node.Id", "a=b"));
	}

	@Test
	public void itDoesTypeAndList() throws Exception {
		testRoundTrip("REMOTE:l=li%3Ast%2FId", new ListRef(ListType.REMOTE, "li:st/Id", null, null));
	}

	@Test
	public void itDoesTypeAndListAndSearch() throws Exception {
		testRoundTrip("GRPC:l=li%3Ast%2FId&s=my+search%26term+foo%3Dbar+etc", new ListRef(ListType.GRPC, "li:st/Id", null, "my search&term foo=bar etc"));
	}

	@Test
	public void itNullForNull() throws Exception {
		assertEquals(null, ListRef.fromUrlForm(null));
	}

	@Test
	public void itThrowsOnVariousInvalid() throws Exception {
		testInvalid("wrong");
		testInvalid("wrong:");
		testInvalid("wrong:a=b");
		testInvalid(":");
		testInvalid("");
		testInvalid("LOCAL");
		testInvalid("LOCAL:l");
		testInvalid("LOCAL:l=");
	}

	private static void testRoundTrip(final String urlForm, final ListRef objForm) {
		assertEquals(urlForm, objForm.toUrlForm());
		assertEquals(objForm, ListRef.fromUrlForm(urlForm));
	}

	private static void testInvalid(final String urlFrom) {
		try {
			ListRef.fromUrlForm(urlFrom);
			Assert.fail("Expected exception for: " + urlFrom);
		}
		catch (final IllegalArgumentException e) {
			// expected.
		}
	}

}
