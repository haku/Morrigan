package com.vaguehope.morrigan.server;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class MorriganServerTest {

	@Test
	public void itRewritesReverseProxyPaths() throws Exception {
		testRewrite("/", "/");
		testRewrite("/foo", "/foo");

		testRewrite("/mn", "/");
		testRewrite("/mn/", "/");
		testRewrite("/mn/foo", "/foo");
		testRewrite("/mn/foo/", "/foo/");
		testRewrite("/mn/foo/bat", "/foo/bat");

		testRewrite("/mn/mlists/LOCALMMDB/a.local.db3/query/t%3D\"Foo%2FBar Bat\"?&column=date_added&order=desc&_=1657822549184",
				"/mlists/LOCALMMDB/a.local.db3/query/t%3D\"Foo%2FBar Bat\"?&column=date_added&order=desc&_=1657822549184");
	}

	private static void testRewrite(final String input, final String expected) throws Exception {
		final Handler wrapped = mock(Handler.class);
		final RewriteHandler rewrites = MorriganServer.wrapWithRewrites(wrapped);
		rewrites.start();
		final Request baseRequest = mock(Request.class);
		final HttpServletResponse resp = mock(HttpServletResponse.class);
		when(baseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
		when(baseRequest.getRequestURI()).thenReturn(input);
		rewrites.handle(input, baseRequest, baseRequest, resp);

		verify(wrapped).handle(expected, baseRequest, baseRequest, resp);
		verify(baseRequest, never()).setURIPathQuery(anyString());
		verify(baseRequest, never()).setPathInfo(anyString());
	}

}
