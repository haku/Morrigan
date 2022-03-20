package com.vaguehope.morrigan.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

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

		final String threadName = "t" + new Random().nextInt(100000000);
		final CountDownLatch latch = new CountDownLatch(1);
		new Thread(threadName) {
			@Override
			public void run() {
				LOG.info(msg);
				latch.countDown();
			}
		}.start();
		latch.await();

		this.undertest.doGet(this.req, this.resp);
		assertThat(this.resp.getOutputAsString(), containsString(msg));
		assertThat(this.resp.getOutputAsString(), containsString(threadName));
	}

}
