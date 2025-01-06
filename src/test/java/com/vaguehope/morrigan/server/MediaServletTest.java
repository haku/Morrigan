package com.vaguehope.morrigan.server;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.common.servlet.MockHttpServletRequest;
import com.vaguehope.common.servlet.MockHttpServletResponse;
import com.vaguehope.morrigan.config.Config;
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

		this.testDb = new TestMixedMediaDb("server-test-db");
		this.mediaFactory.addLocalMixedMediaDb(this.testDb);

		this.undertest = new MediaServlet(this.mediaFactory);

		this.req = new MockHttpServletRequest();
		this.resp = new MockHttpServletResponse();
	}

	@Test
	public void itServesLists() throws Exception {
		this.req.setPathInfo(null);
		this.undertest.service(this.req, this.resp);
		assertEquals("[{\"name\":\"server-test-db\",\"id\":\"test:server-test-db\"}]\n", this.resp.getOutputAsString());
	}

}
