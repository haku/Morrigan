package com.vaguehope.morrigan.server;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.common.servlet.MockHttpServletRequest;
import com.vaguehope.common.servlet.MockHttpServletResponse;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.MediaNode;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.internal.MediaFactoryImpl;
import com.vaguehope.morrigan.model.media.test.TestMixedMediaDb;

public class MediaServletTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private Config config;
	private MediaFactoryImpl mediaFactory;
	private TestMixedMediaDb testDb;

	private MediaServlet undertest;

	private MockHttpServletRequest req;
	private MockHttpServletResponse resp;


	@Before
	public void before() throws Exception {
		this.config = new Config(this.tmp.getRoot());
		this.mediaFactory = new MediaFactoryImpl(this.config, null);

		this.testDb = new TestMixedMediaDb("media-servlet-test-db");
		this.mediaFactory.addLocalMixedMediaDb(this.testDb);

		this.undertest = new MediaServlet(this.mediaFactory);

		this.req = new MockHttpServletRequest();
		this.resp = new MockHttpServletResponse();
	}

	@Test
	public void itServesLists() throws Exception {
		this.req.setPathInfo(null);
		this.undertest.service(this.req, this.resp);
		assertOkJson("[{\"name\":\"media-servlet-test-db\",\"mid\":\"LOCALMMDB:media-servlet-test-db\"}]\n");
	}

	@Test
	public void itServesRootNode() throws Exception {
		this.testDb.addNode(new MediaNode("node-id", "node tile", "0"));
		IMediaItem track = this.testDb.addTestTrack(new File("/path/file.mp3"), new BigInteger("1"), new BigInteger("2"), 1234567890000L, 1234567890000L);
		this.testDb.addTag(track, "some-tag", MediaTagType.MANUAL, (String) null);
		this.req.setPathInfo("/LOCALMMDB:media-servlet-test-db");
		this.undertest.service(this.req, this.resp);
		assertOkJson("{\"nodes\":[{\"id\":\"node-id\",\"title\":\"node tile\",\"parentId\":\"0\"}],"
				+ "\"items\":["
				+ "{\"title\":\"file.mp3\",\"size\":0,\"added\":\"Feb 13, 2009, 11:31:30 PM\",\"mimetype\":\"audio/mpeg\",\"enabled\":true,\"starts\":0,\"ends\":0,\"lastplayed\":\"Feb 13, 2009, 11:31:30 PM\","
				+ "\"tags\":[{\"t\":\"some-tag\"}]}"
				+ "]}\n");
	}

	@Test
	public void itServesSubNode() throws Exception {

	}

	@Test
	public void itServesItem() throws Exception {

	}

	@Test
	public void itServesSearch() throws Exception {

	}


	private void assertOkJson(String expectedJson) throws IOException {
		assertEquals(expectedJson, this.resp.getOutputAsString());
		assertEquals(200, this.resp.getStatus());
	}

}
