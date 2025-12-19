package com.vaguehope.morrigan.model.media;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.Assert;
import org.junit.Test;

public class ListRefTest {

	@Test
	public void itDoesLocal() throws Exception {
		testRoundTrip("LOCAL:l=bar", ListRef.forLocal("bar"));
		testRoundTrip("LOCAL:l=bar&s=a%3Db", ListRef.forLocalSearch("bar", "a=b"));
	}

	@Test
	public void itDoesDlna() throws Exception {
		testRoundTrip("DLNA:l=li%3Ast%2FId&n=some%3Anode%2Fid", ListRef.forDlnaNode("li:st/Id", "some:node/id"));
	}

	@Test
	public void itDoesRpc() throws Exception {
		testRoundTrip("RPC:l=li%3Ast%2FId&n=some%3Anode%2Fid", ListRef.forRpcNode("li:st/Id", "some:node/id"));
		testRoundTrip("RPC:l=li%3Ast%2FId&s=my+search%26term+foo%3Dbar+etc", ListRef.forRpcSearch("li:st/Id", "my search&term foo=bar etc"));
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

	@Test
	public void itThrowsOnMalicious() throws Exception {
		testInvalid("LOCAL:l=.");
		testInvalid("LOCAL:l=..");
		testInvalid("LOCAL:l=../foo");
		testInvalid("LOCAL:l=../../some/other/path");
	}

	@Test
	public void itSorts() throws Exception {
		assertEquals(0, ListRef.forRpcNode("id", "0").compareTo(ListRef.forRpcNode("id", "0")));

		assertEquals(-1, ListRef.forRpcNode("a", "0").compareTo(ListRef.forRpcNode("b", "0")));
		assertEquals(1, ListRef.forRpcNode("b", "0").compareTo(ListRef.forRpcNode("a", "0")));

		assertEquals(-1, ListRef.forRpcNode("a", null).compareTo(ListRef.forRpcNode("b", "0")));
		assertEquals(1, ListRef.forRpcNode("b", "0").compareTo(ListRef.forRpcNode("a", null)));

		assertEquals(-1, ListRef.forRpcNode("a", "1").compareTo(ListRef.forRpcNode("b", "2")));
		assertEquals(1, ListRef.forRpcNode("b", "2").compareTo(ListRef.forRpcNode("a", "1")));
	}

	@Test
	public void itWorksAsMapKey() throws Exception {
		final ListRef ref = ListRef.forRpcNode("id", "0");
		final Object val = new Object();
		final Map<ListRef, Object> cslp = new ConcurrentSkipListMap<>();
		cslp.put(ref, val);
		assertEquals(val, cslp.get(ref));
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
