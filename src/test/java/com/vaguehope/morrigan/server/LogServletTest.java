package com.vaguehope.morrigan.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogServletTest {

	private static final Logger LOG = LoggerFactory.getLogger(LogServletTest.class);

	private MockHttpServletRequest req;
	private MockHttpServletResponse resp;
	private LogServlet undertest;

	@Before
	public void before() throws Exception {
		this.req = new MockHttpServletRequest();
		this.resp = new MockHttpServletResponse();
		this.undertest = new LogServlet();
	}

	@Test
	public void itDoesSomething() throws Exception {
		final String msg = "test message " + System.currentTimeMillis();
		LOG.info(msg);
		this.undertest.doGet(this.req, this.resp);
		assertThat(this.resp.getOutputAsString(), containsString(msg));
	}

}
