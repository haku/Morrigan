package com.vaguehope.morrigan.sshui.util;

public interface Submenu {
	boolean isSelectable();

	int size();

	/**
	 * May return null while size() == 1 so "(empty)" placeholder can be drawn.
	 */
	Object get(final int index);

	/**
	 * Return -1 if not found.
	 */
	int indexOf(Object item);

	String stringForItem(final int index);
}