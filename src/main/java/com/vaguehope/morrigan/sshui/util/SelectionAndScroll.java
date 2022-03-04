package com.vaguehope.morrigan.sshui.util;

public class SelectionAndScroll {

	public final Object selectedItem;
	public final int scrollTop;

	public SelectionAndScroll(final Object selectedItem, final int scrollTop) {
		this.selectedItem = selectedItem;
		this.scrollTop = scrollTop;
	}

	public SelectionAndScroll withSelectedItem(Object item) {
		return new SelectionAndScroll(item, this.scrollTop);
	}

}
