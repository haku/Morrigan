package morrigan.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import morrigan.config.SavedView;

public class SavedViewTest {

	@Test
	public void itDoesSomething() throws Exception {
		final SavedView a = new SavedView("a", "b", "c");
		final SavedView b = new SavedView("a", "b", "c");
		assertEquals(a, b);
	}

}
