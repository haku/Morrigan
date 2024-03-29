package com.vaguehope.morrigan.sshui;

import java.util.Collection;
import java.util.List;

public final class MenuHelper {

	public enum VDirection {
		UP, DOWN;
	}

	private MenuHelper () {
		throw new AssertionError();
	}

	public static int moveListSelectionIndex (final int selectedIndex, final VDirection direction, final int distance, final List<?> list) {
		return moveListSelectionIndex(selectedIndex, direction, distance, sizeOf(list));
	}

	private static int moveListSelectionIndex (final int selectedIndex, final VDirection direction, final int distance, final int limit) {
		int i = selectedIndex;
		if (i < 0) {
			i = limit > 0 ? 0 : -1;
		}
		else if (direction == VDirection.UP) {
			i -= distance;
			if (i < 0) i = 0;
		}
		else {
			i += distance;
			if (i >= limit) i = limit - 1;
		}
		return i;
	}

	/**
	 * Returns new selected item.
	 */
	public static Object moveListSelection (final Object selectedItem, final VDirection direction, final int distance, final List<?>... lists) {
		final int limit = sumSizes(lists);
		int i = listOfListsIndexOf(selectedItem, lists);
		if (i < 0) {
			i = limit > 0 ? 0 : -1;
		}
		else if (direction == VDirection.UP) {
			i -= distance;
			if (i < 0) i = 0;
		}
		else {
			i += distance;
			if (i >= limit) i = limit - 1;
		}
		return listOfListsGet(i, lists);
	}

	/**
	 *
	 * @param topLine how far down the screen the list starts.
	 * @param scrollTop previous ScrollTop value.
	 * @param selectedItemIndex item position from the first item in the list.
	 * @return
	 */
	public static int calcScrollTop(final int visibleRows, final int scrollTopIndex, final int selectedItemIndex) {
		if (selectedItemIndex < 0) {
			return scrollTopIndex;
		}
		final int maxOnScreenIndex = visibleRows - 1;
		if (selectedItemIndex - scrollTopIndex > maxOnScreenIndex) {
			return selectedItemIndex - maxOnScreenIndex;
		}
		else if (selectedItemIndex < scrollTopIndex) {
			return selectedItemIndex;
		}
		return scrollTopIndex;
	}

	public static int sumSizes (final List<?>... lists) {
		int count = 0;
		for (final List<?> list : lists) {
			count += sizeOf(list);
		}
		return count;
	}

	private static int listOfListsIndexOf (final Object item, final List<?>... lists) {
		if (item == null) return -1;
		int offset = 0;
		for (final List<?> list : lists) {
			final int x = list != null ? list.indexOf(item) : -1;
			if (x >= 0) return offset + x;
			offset += sizeOf(list);
		}
		return -1;
	}

	public static Object listOfListsGet (final int i, final List<?>... lists) {
		if (i < 0) return null;
		int x = i;
		for (final List<?> list : lists) {
			if (x < sizeOf(list)) return listGet(list, x);
			x -= sizeOf(list);
		}
		return null;
	}

	public static <T> int sizeOf (final T[] c) {
		return c != null ? c.length : 0;
	}

	public static int sizeOf (final Collection<?> c) {
		return c != null ? c.size() : 0;
	}

	public static <T> T listGet (final List<T> list, final int i) {
		if (list == null || i < 0) return null;
		return list.get(i);
	}

}
