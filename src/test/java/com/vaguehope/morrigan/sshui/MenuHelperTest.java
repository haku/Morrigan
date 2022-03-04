package com.vaguehope.morrigan.sshui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MenuHelperTest {

	@Test
	public void itDoesSomething() throws Exception {
		assertEquals(0, MenuHelper.calcScrollTop(10, 0, 0));
		assertEquals(0, MenuHelper.calcScrollTop(10, 0, 1));
		assertEquals(0, MenuHelper.calcScrollTop(10, 0, 8));
		assertEquals(0, MenuHelper.calcScrollTop(10, 0, 9));
		assertEquals(1, MenuHelper.calcScrollTop(10, 0, 10));
		assertEquals(0, MenuHelper.calcScrollTop(10, 1, 0));
		assertEquals(1, MenuHelper.calcScrollTop(10, 1, 1));
	}

}
