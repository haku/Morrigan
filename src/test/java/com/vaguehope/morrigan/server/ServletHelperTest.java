package com.vaguehope.morrigan.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.util.IoHelper;

public class ServletHelperTest {

	private static final String FILE_CONTENT = "1234567890abcdefghijklmnopqrstuvwxyz";

	/*
	 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Range
	 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Range
	 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/206
	 *
	 * > Range:
	 * < Content-Length: 124139277
	 *
	 * > Range: bytes=0-
	 * < Content-Length: 124139277
	 * < Content-Range: bytes 0-124139276/124139277
	 *
	 * > Range: bytes=62652416-
	 * < Content-Length: 61486861
	 * < Content-Range: bytes 62652416-124139276/124139277
	 */

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private MockHttpServletResponse resp;
	private File inputFile;

	@Before
	public void before () throws Exception {
		this.resp = new MockHttpServletResponse();
		this.inputFile = this.tmp.newFile("a");
		IoHelper.write(FILE_CONTENT, this.inputFile);
	}

	@Test
	public void itGetsReqPath() throws Exception {
		testRepPath("mlists", "/mlists", "");
		testRepPath("mlists", "/mlists/foo", "/foo");
		testRepPath("mlists", "/mn/mlists/foo", "/foo");
		testRepPath("mlists", "mlists", "");
		testRepPath("mlists", "mlists/foo", "/foo");
		testRepPath("mlists", "mlists/foo", "/foo");
	}

	private static void testRepPath(String relativeContextPath, final String input, final String expected) {
		final HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getRequestURI()).thenReturn(input);
		assertEquals(expected, ServletHelper.getReqPath(req, relativeContextPath));
	}

	@Test
	public void itHandlesNoRangeHeader () throws Exception {
		ServletHelper.returnFile(this.inputFile, null, null, "", this.resp);
		assertEquals(FILE_CONTENT, this.resp.getOutputAsString());
		assertEquals(String.valueOf(FILE_CONTENT.length()), this.resp.headers.get("Content-Length"));
		assertNull(this.resp.headers.get("Content-Range"));
		assertEquals(200, this.resp.status);
	}

	@Test
	public void itHandlesInvalidRangeHeader () throws Exception {
		final String[][] inputs = new String[][] {
				new String[] { "foo=123-456", "HTTP Error 400: Unsupported Range header, wrong units.\n" },
				new String[] { "bytes=0-1", "HTTP Error 400: Unsupported Range header, not a single range.\n" },
				new String[] { "bytes=123-456", "HTTP Error 400: Unsupported Range header, not a single range.\n" },
				new String[] { "bytes=-456", "HTTP Error 400: Unsupported Range header, not a single range.\n" },
				new String[] { "bytes=abc-", "HTTP Error 400: Unsupported Range header, not a number.\n" },
		};
		for (final String[] input : inputs) {
			ServletHelper.returnFile(this.inputFile, null, null, input[0], this.resp);
			assertEquals(input[1], this.resp.getOutputAsString());
			assertEquals(400, this.resp.status);
		}
	}

	@Test
	public void itHandlesZeroRangeHeader () throws Exception {
		ServletHelper.returnFile(this.inputFile, null, null, "bytes=0-", this.resp);
		assertEquals(String.valueOf(FILE_CONTENT.length()), this.resp.headers.get("Content-Length"));
		assertEquals(String.format("bytes 0-%s/%s",
				String.valueOf(FILE_CONTENT.length() - 1),
				String.valueOf(FILE_CONTENT.length())),
				this.resp.headers.get("Content-Range"));
		assertEquals(206, this.resp.status);
	}

	@Test
	public void itHandlesOffsetRangeHeader () throws Exception {
		ServletHelper.returnFile(this.inputFile, null, null, "bytes=13-", this.resp);
		assertEquals(String.valueOf(FILE_CONTENT.length() - 13), this.resp.headers.get("Content-Length"));
		assertEquals(String.format("bytes 13-%s/%s",
				String.valueOf(FILE_CONTENT.length() - 1),
				String.valueOf(FILE_CONTENT.length())),
				this.resp.headers.get("Content-Range"));
		assertEquals(206, this.resp.status);
	}

}
