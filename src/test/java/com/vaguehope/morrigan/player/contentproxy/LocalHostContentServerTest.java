package com.vaguehope.morrigan.player.contentproxy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalHostContentServerTest {

	private LocalHostContentServer undertest;

	@Before
	public void before() throws Exception {
		this.undertest = new LocalHostContentServer(true);
	}

	@After
	public void after() throws Exception {
		this.undertest.shutdown();
	}

	@SuppressWarnings("resource")
	@Test
	public void itServesStuff() throws Exception {
		this.undertest.start();
		assertThat(this.undertest.getExternalHttpUrl(), matchesRegex("^http://127\\.0\\.0\\.1:[0-9]{5}$"));

		final ContentServer contentServer = mock(ContentServer.class);
		doAnswer((inv) -> {
			inv.getArgument(1, HttpServletResponse.class).getWriter().println("012345");
			return null;
		}).when(contentServer).doGet(isA(HttpServletRequest.class), isA(HttpServletResponse.class), eq("list-id"), eq("item-id"));

		final String uri = this.undertest.makeUri(contentServer, "list-id", "item-id");
		final HttpURLConnection conn = (HttpURLConnection) new URL(uri).openConnection();
		assertEquals(200, conn.getResponseCode());
		assertEquals("012345\n", IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8));
	}

}
