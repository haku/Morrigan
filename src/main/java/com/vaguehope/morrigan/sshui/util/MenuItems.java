package com.vaguehope.morrigan.sshui.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.vaguehope.morrigan.sshui.MenuHelper;
import com.vaguehope.morrigan.sshui.MenuHelper.VDirection;

public class MenuItems {

	private final List<Submenu> parts;

	private MenuItems(final List<Submenu> parts) {
		this.parts = parts;
	}

	public int size() {
		int count = 0;
		for (final Submenu p : this.parts) {
			count += p.size();
		}
		return count;
	}

	public MenuItem get(final int index) {
		return getSubMenuAndIndex(index);
	}

	/**
	 * Return -1 if not found.
	 */
	public int indexOf(final Object item) {
		if (item == null) return -1;
		int offset = 0;
		for (final Submenu p : this.parts) {
			final int x = p.indexOf(item);
			if (x >= 0) return offset + x;
			offset += p.size();
		}
		return -1;
	}

	/**
	 * Returns newly selected item.
	 */
	public SelectionAndScroll moveSelection(final SelectionAndScroll prev, final int visibleRows, final int distance) {
		if (distance == 0) return prev;

		final int ulimit = size() - 1;
		int goalSelI = indexOf(prev.selectedItem);
		goalSelI = goalSelI + distance;

		if (goalSelI > ulimit) goalSelI = ulimit;
		if (goalSelI < 0) goalSelI = 0;

		int goalRevealI;
		if (distance > 0) {
			goalRevealI = prev.scrollTop + visibleRows - 1 + distance;
			if (goalRevealI > ulimit) goalRevealI = ulimit;
		}
		else {
			goalRevealI = prev.scrollTop + distance;
		}

		return adjustSelection(prev, visibleRows, distance > 0 ? VDirection.DOWN : VDirection.UP, ulimit, goalSelI, goalRevealI);
	}

	public SelectionAndScroll moveSelectionToEnd(final SelectionAndScroll prev, final int visibleRows, final VDirection direction) {
		final int ulimit = size() - 1;
		final int goalSelI = direction == VDirection.DOWN ? ulimit : 0;
		final int goalTop = direction == VDirection.DOWN ? ulimit : 0;
		return adjustSelection(prev, visibleRows, direction, ulimit, goalSelI, goalTop);
	}

	private SelectionAndScroll adjustSelection(
			final SelectionAndScroll prev,
			final int visibleRows,
			final VDirection direction,
			final int ulimit,
			final int goalSelI,
			final int goalRevealI) {
		final SubMenuAndIndex s = getSubMenuAndIndex(goalSelI);
		if (s != null && s.hasSelectableItem()) return new SelectionAndScroll(s.getItem(), MenuHelper.calcScrollTop(visibleRows, prev.scrollTop, s.overallIndex));

		if (direction == VDirection.DOWN) {
			if (goalSelI < ulimit) {
				for (int x = goalSelI + 1; x <= ulimit; x++) {
					final SubMenuAndIndex t = getSubMenuAndIndex(x); // NPE
					if (t != null && t.hasSelectableItem())
						return new SelectionAndScroll(t.getItem(), MenuHelper.calcScrollTop(visibleRows, prev.scrollTop, t.overallIndex));
				}
			}
			for (int x = goalSelI - 1; x >= 0; x--) {
				final SubMenuAndIndex t = getSubMenuAndIndex(x); // NPE
				if (t != null && t.hasSelectableItem())
					return new SelectionAndScroll(t.getItem(), MenuHelper.calcScrollTop(visibleRows, prev.scrollTop, goalRevealI));
			}
		}
		else {
			if (goalSelI > 0) {
				for (int x = goalSelI - 1; x >= 0; x--) {
					final SubMenuAndIndex t = getSubMenuAndIndex(x); // NPE
					if (t != null && t.hasSelectableItem())
						return new SelectionAndScroll(t.getItem(), MenuHelper.calcScrollTop(visibleRows, prev.scrollTop, t.overallIndex));
				}
			}
			for (int x = goalSelI + 1; x <= ulimit; x++) {
				final SubMenuAndIndex t = getSubMenuAndIndex(x);
				if (t != null && t.hasSelectableItem())
					return new SelectionAndScroll(t.getItem(), MenuHelper.calcScrollTop(visibleRows, prev.scrollTop, goalRevealI));
			}
		}

		return prev;
	}

	private SubMenuAndIndex getSubMenuAndIndex(final int index) {
		if (index < 0) return null;
		int x = index;
		for (final Submenu p : this.parts) {
			if (x < p.size()) return new SubMenuAndIndex(p, x, index);
			x -= p.size();
		}
		return null;
	}

	private static class SubMenuAndIndex implements MenuItem {
		public final Submenu submenu;
		public final int subIndex;
		public final int overallIndex;

		public SubMenuAndIndex(final Submenu submenu, final int subIndex, final int overallIndex) {
			this.submenu = submenu;
			this.subIndex = subIndex;
			this.overallIndex = overallIndex;
		}

		public boolean hasSelectableItem() {
			if (!this.submenu.isSelectable()) return false;
			return getItem() != null;
		}

		@Override
		public Object getItem() {
			return this.submenu.get(this.subIndex);
		}

		@Override
		public String toString() {
			return this.submenu.stringForItem(this.subIndex);
		}
	}

	private static class DefaultSubmenu<T> implements Submenu {

		private final List<T> list;
		private final boolean selectable;
		private final String emptyMsg;
		private final Function<T, String> toString;

		public DefaultSubmenu(final List<T> list, final boolean selectable, final String emptyMsg, final Function<T, String> toString) {
			this.list = list;
			this.selectable = selectable;
			this.emptyMsg = emptyMsg;
			this.toString = toString;
		}

		@Override
		public boolean isSelectable() {
			return this.selectable;
		}

		@Override
		public int size() {
			final int s = this.list.size();
			if (s > 0) return s;
			return 1;
		}

		@Override
		public Object get(final int index) {
			if (this.list.size() < 1) return null;
			return this.list.get(index);
		}

		@Override
		public int indexOf(final Object item) {
			return this.list.indexOf(item);
		}

		@Override
		public String stringForItem(final int index) {
			if (this.list.size() < 1) return this.emptyMsg;
			return this.toString.apply(this.list.get(index));
		}

	}

	public static class MenuBuilder {

		private final List<Submenu> submenus = new ArrayList<>();

		public MenuBuilder addHeading(final String heading) {
			return addSubmenu(new DefaultSubmenu<>(Collections.singletonList(heading), false, "", a -> a));
		}

		public <T> MenuBuilder addList(final List<T> list, final String emptyMsg, final Function<T, String> toString) {
			return addSubmenu(new DefaultSubmenu<>(list, true, emptyMsg, toString));
		}

		public MenuBuilder addSubmenu(final Submenu submenu) {
			this.submenus.add(submenu);
			return this;
		}

		public MenuItems build() {
			return new MenuItems(this.submenus);
		}
	}

	public static MenuBuilder builder() {
		return new MenuBuilder();
	}

}
