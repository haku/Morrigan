package com.vaguehope.morrigan.sshui.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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
	public Object moveSelection(final Object selectedItem, final int distance) {
		if (distance == 0) return selectedItem;

		final int ulimit = size() - 1;
		int i = indexOf(selectedItem);
		i = i + distance;

		if (i > ulimit) i = ulimit;
		if (i < 0) i = 0;

		return adjustSelection(selectedItem, distance > 0 ? VDirection.DOWN : VDirection.UP, ulimit, i);
	}

	public Object moveSelectionToEnd(final Object selectedItem, final VDirection direction) {
		final int ulimit = size() - 1;
		return adjustSelection(selectedItem, direction, ulimit, direction == VDirection.DOWN ? ulimit : 0);
	}

	private Object adjustSelection(final Object selectedItem, final VDirection direction, final int ulimit, int i) {
		final SubMenuAndIndex s = getSubMenuAndIndex(i);
		if (s.hasSelectableItem()) return s.getItem();

		if (direction == VDirection.DOWN) {
			if (i < ulimit) {
				for (int x = i + 1; x <= ulimit; x++) {
					final SubMenuAndIndex t = getSubMenuAndIndex(x);
					if (t.hasSelectableItem()) return t.getItem();
				}
			}
			for (int x = i - 1; x >= 0; x--) {
				final SubMenuAndIndex t = getSubMenuAndIndex(x);
				if (t.hasSelectableItem()) return t.getItem();
			}
		}
		else {
			if (i > 0) {
				for (int x = i - 1; x >= 0; x--) {
					final SubMenuAndIndex t = getSubMenuAndIndex(x);
					if (t.hasSelectableItem()) return t.getItem();
				}
			}
			for (int x = i + 1; i <= ulimit; x++) {
				final SubMenuAndIndex t = getSubMenuAndIndex(x);
				if (t.hasSelectableItem()) return t.getItem();
			}
		}
		return selectedItem;
	}

	private SubMenuAndIndex getSubMenuAndIndex(final int index) {
		if (index < 0) return null;
		int x = index;
		for (final Submenu p : this.parts) {
			if (x < p.size()) return new SubMenuAndIndex(p, x);
			x -= p.size();
		}
		return null;
	}

	private static class SubMenuAndIndex implements MenuItem {
		public final Submenu submenu;
		public final int index;

		public SubMenuAndIndex(final Submenu submenu, final int index) {
			this.submenu = submenu;
			this.index = index;
		}

		public boolean hasSelectableItem() {
			if (!this.submenu.isSelectable()) return false;
			return getItem() != null;
		}

		@Override
		public Object getItem() {
			return this.submenu.get(this.index);
		}

		@Override
		public String toString() {
			return this.submenu.stringForItem(this.index);
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
