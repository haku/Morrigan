package com.vaguehope.morrigan.android.helper;

import static org.junit.Assert.*;

import org.junit.Test;

public class UriHelperTest {

	@Test
	public void itRemovesTrailingSlash () throws Exception {
		assertEquals("http://host/path", UriHelper.ensureNoTrailingSlash("http://host/path"));
		assertEquals("http://host/path", UriHelper.ensureNoTrailingSlash("http://host/path/"));
	}

	@Test
	public void itJointsParts () throws Exception {
		assertEquals("http://host/path/foo/bar", UriHelper.joinParts("http://host/path/", "foo", "bar"));
		assertEquals("http://host/path/foo/bar", UriHelper.joinParts("http://host/path/", "/foo", "bar/"));
		assertEquals("http://host/path/foo/bar", UriHelper.joinParts("http://host/path/", "/foo/", "/bar/"));
	}

}
