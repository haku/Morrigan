package com.vaguehope.morrigan.config;

import static org.junit.Assert.*;

import org.junit.Test;

public class SavedViewTest {

	@Test
	public void itDoesSomething() throws Exception {
		final SavedView a = new SavedView("a", "b", "c");
		final SavedView b = new SavedView("a", "b", "c");
		assertEquals(a, b);
	}

}
