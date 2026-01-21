package morrigan.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import morrigan.server.PathAndSubPath;

public class PathAndSubPathTest {

	@Test
	public void itParses() throws Exception {
		assertEquals(new PathAndSubPath(null, null), PathAndSubPath.split(null));
		assertEquals(new PathAndSubPath(null, null), PathAndSubPath.split(""));
		assertEquals(new PathAndSubPath(null, null), PathAndSubPath.split("/"));
		assertEquals(new PathAndSubPath("foo", null), PathAndSubPath.split("/foo"));
		assertEquals(new PathAndSubPath("foo", null), PathAndSubPath.split("foo"));
		assertEquals(new PathAndSubPath("foo", null), PathAndSubPath.split("/foo/"));
		assertEquals(new PathAndSubPath("foo", null), PathAndSubPath.split("foo/"));
		assertEquals(new PathAndSubPath("foo", "bar"), PathAndSubPath.split("/foo/bar"));
		assertEquals(new PathAndSubPath("foo", "bar"), PathAndSubPath.split("/foo/bar/"));
		assertEquals(new PathAndSubPath("foo", "bar"), PathAndSubPath.split("foo/bar"));
		assertEquals(new PathAndSubPath("foo", "bar"), PathAndSubPath.split("foo/bar/"));
		assertEquals(new PathAndSubPath("foo", "bar/bat"), PathAndSubPath.split("/foo/bar/bat"));
		assertEquals(new PathAndSubPath("foo", "bar/bat"), PathAndSubPath.split("foo/bar/bat"));
		assertEquals(new PathAndSubPath(null, "bar"), PathAndSubPath.split("//bar"));
		assertEquals(new PathAndSubPath(null, "bar"), PathAndSubPath.split("//bar/"));
		assertEquals(new PathAndSubPath(null, "bar/bat"), PathAndSubPath.split("//bar/bat"));
	}


}
