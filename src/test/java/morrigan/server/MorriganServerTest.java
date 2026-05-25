package morrigan.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

import com.vaguehope.common.servlet.MockHttpServletResponse;

public class MorriganServerTest {

	@Test
	public void itRedirectsToAddSlash() throws Exception {
		testRedirect("/mn", "/mn/", "GET");
		testRedirect("/mn", "/mn", "OPTIONS");
	}

	@Test
	public void itRewritesReverseProxyPaths() throws Exception {
		testRewrite("/", "/");
		testRewrite("/foo", "/foo");

		testRewrite("/mn/", "/");
		testRewrite("/mn/foo", "/foo");
		testRewrite("/mn/foo/", "/foo/");
		testRewrite("/mn/foo/bat", "/foo/bat");

		testRewrite("/mn/mlists/LOCALMMDB/a.local.db3/query/t%3D\"Foo%2FBar Bat\"?&column=date_added&order=desc&_=1657822549184",
				"/mlists/LOCALMMDB/a.local.db3/query/t%3D\"Foo%2FBar Bat\"?&column=date_added&order=desc&_=1657822549184");
	}

	private static void testRedirect(final String input, final String expected, final String method) throws Exception {
		testRedirectOrRewrite(input, expected, false, method);
	}

	private static void testRewrite(final String input, final String expected) throws Exception {
		testRedirectOrRewrite(input, expected, true, "GET");
	}

	private static void testRedirectOrRewrite(final String input, final String expected, final boolean isRewrite, final String method) throws Exception {
		final Request baseRequest = mock(Request.class);
		when(baseRequest.getMethod()).thenReturn(method);
		when(baseRequest.getScheme()).thenReturn("https");
		when(baseRequest.getServerName()).thenReturn("mn.example.com");
		when(baseRequest.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
		when(baseRequest.getRequestURI()).thenReturn(input);

		final Handler wrapped = mock(Handler.class);
		final RewriteHandler rewrites = MorriganServer.wrapWithRewrites(wrapped);
		rewrites.start();

		final HttpServletResponse resp = new MockHttpServletResponse();
		rewrites.handle(input, baseRequest, baseRequest, resp);

		if (isRewrite) {
			verify(wrapped).handle(expected, baseRequest, baseRequest, resp);
		}
		else if (input.equals(expected)) {
			assertEquals(200, resp.getStatus());
			assertNull(resp.getHeader("Location"));
		}
		else {
			assertEquals(301, resp.getStatus());
			assertEquals("https://mn.example.com" + expected, resp.getHeader("Location"));
		}

		verify(baseRequest, never()).setURIPathQuery(anyString());
		verify(baseRequest, never()).setPathInfo(anyString());
	}

}
