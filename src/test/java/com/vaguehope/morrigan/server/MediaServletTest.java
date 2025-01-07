package com.vaguehope.morrigan.server;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableList;
import com.vaguehope.common.servlet.MockHttpServletRequest;
import com.vaguehope.common.servlet.MockHttpServletResponse;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.model.media.IMediaItemList;
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
		final IMediaItem track = this.testDb.addTestTrack(new File("/path/file.mp3"), new BigInteger("1"), new BigInteger("2"), 1234567890000L, 1234567890000L);
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
	public void it404sForUnknownNode() throws Exception {
		this.req.setPathInfo("/LOCALMMDB:media-servlet-test-db/node/unknown");
		this.undertest.service(this.req, this.resp);
		assertEquals(404, this.resp.getStatus());
	}

	@Test
	public void itServesSubNode() throws Exception {
		final IMediaItemList node = mock(IMediaItemList.class);
		when(node.hasNodes()).thenReturn(true);
		when(node.getSubNodes()).thenReturn(ImmutableList.of(new MediaNode("sub-node-id", "node tile", "my-node")));
		this.testDb.addChildNode("my-node", node);
		this.req.setPathInfo("/LOCALMMDB:media-servlet-test-db/node/my-node");
		this.undertest.service(this.req, this.resp);
		assertOkJson("{\"nodes\":[{\"id\":\"sub-node-id\",\"title\":\"node tile\",\"parentId\":\"my-node\"}],\"items\":[]}\n");
	}

	@Test
	public void itServesSearch() throws Exception {

	}

	@Test
	public void itServesLocalItem() throws Exception {
		final byte[] data = "content".getBytes();
		final File file = this.tmp.newFile();
		FileUtils.writeByteArrayToFile(file, data);
		this.testDb.addTestTrack(file);

		this.req.setPathInfo("/LOCALMMDB:media-servlet-test-db/item/" + file.getAbsolutePath());
		this.undertest.service(this.req, this.resp);
		assertArrayEquals(data, this.resp.getOutputAsByteArray());
	}

	@Test
	public void itServesTranscodedLocalItem() throws Exception {

	}

	@Test
	public void itServesResizedLocalItem() throws Exception {

	}

	@Test
	public void itServesRemoteItem() throws Exception {

	}

	private void assertOkJson(final String expectedJson) throws IOException {
		assertEquals(expectedJson, this.resp.getOutputAsString());
		assertEquals(200, this.resp.getStatus());
	}

}
