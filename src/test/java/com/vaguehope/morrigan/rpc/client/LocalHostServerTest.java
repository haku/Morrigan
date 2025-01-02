package com.vaguehope.morrigan.rpc.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.mockito.Mockito.mock;

import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LocalHostServerTest {

	private LocalHostServer undertest;
	private HttpServlet servlet;

	@Before
	public void before() throws Exception {
		this.servlet = mock(HttpServlet.class);
		this.undertest = new LocalHostServer(this.servlet, true);
	}

	@After
	public void after() throws Exception {
		this.undertest.shutdown();
	}

	@Test
	public void itDoesSomething() throws Exception {
		this.undertest.start();
		assertThat(this.undertest.getExternalHttpUrl(), matchesRegex("^http://127\\.0\\.0\\.1:[0-9]{5}$"));

		// TODO set servlet is called?
	}

}
