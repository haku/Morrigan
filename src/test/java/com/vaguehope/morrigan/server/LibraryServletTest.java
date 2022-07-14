package com.vaguehope.morrigan.server;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.config.Config;

public class LibraryServletTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private Set<String> libraries;
	private Config config;
	private LibraryServlet undertest;

	private MockHttpServletRequest req;
	private MockHttpServletResponse resp;
	private MockHttpServletRequest req2;
	private MockHttpServletResponse resp2;

	private LocalServer server;

	@Before
	public void before () throws Exception {
		this.server = new LocalServer();

		this.req = new MockHttpServletRequest();
		this.resp = new MockHttpServletResponse();
		this.req2 = new MockHttpServletRequest();
		this.resp2 = new MockHttpServletResponse();

		this.libraries = new HashSet<>();
		this.config = new Config(this.tmp.getRoot());
	}

	@After
	public void stopServer () throws Exception {
		if (this.server != null) this.server.dispose();
	}

	private void startUndertest () throws IOException {
		this.undertest = new LibraryServlet(this.libraries, this.config);
		this.undertest.upstreamScheme = "http";
	}

	@Test
	public void itRemovesScheme () throws Exception {
		final URI uri = new URI("a", "b", "c", 1, "/d", "e", "f");
		assertEquals("c:1/d?e#f", LibraryServlet.withoutScheme(uri));
	}

	@Test
	public void itReqritesUrls() throws Exception {
		testUrlRewrite(
				"https://fonts.googleapis.com/icon?family=Material+Icons",
				"http://fonts.gstatic.com/s/materialicons/v52/flUhRq6tzZclQEJ-Vdg-IuiaDsNc.woff2",
				"http://fonts.gstatic.com/s/materialicons/v52/flUhRq6tzZclQEJ-Vdg-IuiaDsNc.woff2",
				"/lib/fonts.gstatic.com/s/materialicons/v52/flUhRq6tzZclQEJ-Vdg-IuiaDsNc.woff2");

		testUrlRewrite(
				"https://code.jquery.com/ui/1.11.4/themes/dark-hive/jquery-ui.css",
				"https://code.jquery.com/ui/1.11.4/themes/dark-hive/images/ui-bg_loop_25_000000_21x21.png",
				"images/ui-bg_loop_25_000000_21x21.png",
				"/lib/code.jquery.com/ui/1.11.4/themes/dark-hive/images/ui-bg_loop_25_000000_21x21.png");
	}

	private static void testUrlRewrite (final String cssUrl, final String resourceAbsUrl, final String resourceCssUrl, final String expectedReplacementUrl) throws URISyntaxException, IOException {
		final List<String> variations = new ArrayList<>();
		variations.add("url(" + resourceCssUrl + ")");
		variations.add("url(\"" + resourceCssUrl + "\")");
		variations.add("url('" + resourceCssUrl + "')");
		variations.add("url(   " + resourceCssUrl + "   )");
		variations.add("url(   \"" + resourceCssUrl + "\"   )");
		variations.add("url(   '" + resourceCssUrl + "'   )");

		for (final String variation : variations) {
			System.out.println("Testing: >>" + variation + "<<");
			final URI parent = new URI(cssUrl);
			final ByteArrayOutputStream buff = new ByteArrayOutputStream();
			buff.write(variation.getBytes());
			final Map<String, String> rewrites = LibraryServlet.rewriteCss(parent, buff);

			final Map<String, String> expected = new HashMap<>();
			expected.put(resourceAbsUrl, expectedReplacementUrl);
			assertEquals(expected, rewrites);

			assertEquals(1, rewrites.size());
		}
	}

	@Test
	public void itProxiesCssFile () throws Exception {
		final URI lib = rewriteHost("https://fonts.googleapis.com/icon?family=Material+Icons");
		this.libraries.add(lib.toString());
		startUndertest();

		final URI fontUrl = rewriteHost("http://fonts.gstatic.com/s/materialicons/v52/flUhRq6tzZclQEJ-Vdg-IuiaDsNc.woff2");
		final String fontRewrittenPath = "/lib/" + LibraryServlet.withoutScheme(fontUrl);

		final String cssFixture = "@font-face {\n" +
				"  font-family: 'Material Icons';\n" +
				"  font-style: normal;\n" +
				"  font-weight: 400;\n" +
				"  src: url({url}) format('woff2');\n" +
				"}";
		final String responseFixture = cssFixture.replace("{url}", fontUrl.toString());
		final String expectedResponse = cssFixture.replace("{url}", fontRewrittenPath);

		this.req.requestURI = "/lib/" + LibraryServlet.withoutScheme(lib);
		this.server.getFixtureServlet().addGetFixture(lib, "text/css; charset=utf-8", responseFixture);

		this.req2.requestURI = fontRewrittenPath;
		this.server.getFixtureServlet().addGetFixture(fontUrl, "font/woff2", "mock-font");

		this.undertest.service(this.req, this.resp);
		assertEquals(200, this.resp.getStatus());
		assertEquals(expectedResponse, this.resp.getOutputAsString());

		this.undertest.service(this.req2, this.resp2);
		assertEquals(200, this.resp2.getStatus());
		assertEquals("mock-font", this.resp2.getOutputAsString());
	}

	@Test
	public void itRemovesReverseProxyPrefix() throws Exception {
		final URI lib = rewriteHost("https://fonts.googleapis.com/icon?family=Material+Icons");
		this.libraries.add(lib.toString());
		startUndertest();

		this.req.requestURI = "/mn/lib/" + LibraryServlet.withoutScheme(lib);
		this.server.getFixtureServlet().addGetFixture(lib, "text/css; charset=utf-8", "@font-face {}");

		this.undertest.service(this.req, this.resp);
		assertEquals(200, this.resp.getStatus());
	}

	private URI rewriteHost (final String url) throws URISyntaxException {
		final URI o = new URI(url);
		final URI n = this.server.getUri();
		return new URI(n.getScheme(), o.getUserInfo(), n.getHost(), n.getPort(), o.getPath(), o.getQuery(), o.getFragment());
	}

}
