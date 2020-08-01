package com.vaguehope.morrigan.android.playback;

import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.vaguehope.morrigan.android.helper.LogWrapper;

public class ScrollState {

	private static final String KEY_ITEM_ID = "list_view_item_id";
	private static final String KEY_TOP = "list_view_top";

	private static final LogWrapper LOG = new LogWrapper("SS");

	private final long itemId;
	private final int top;

	public ScrollState (final long itemId, final int top) {
		this.itemId = itemId;
		this.top = top;
	}

	@Override
	public String toString () {
		return new StringBuilder()
				.append("ScrollState{").append(this.itemId)
				.append(',').append(this.top)
				.append('}')
				.toString();
	}

	public long getItemId () {
		return this.itemId;
	}

	public int getTop () {
		return this.top;
	}

	public void applyTo (final ListView lv) {
		applyToListView(lv);
	}

	private void applyToListView (final ListView lv) {
		// NOTE if this seems unreliable try wrapping setSelection*() calls in lv.post(...).
		final ListAdapter adapter = lv.getAdapter();

		if (this.itemId >= 0L) {
			for (int i = 0; i < adapter.getCount(); i++) {
				if (adapter.getItemId(i) == this.itemId) {
					lv.setSelectionFromTop(i, this.top);
					return;
				}
			}
		}

		LOG.w("Failed to restore scroll state %s.", this);
	}

	public void addToBundle (final Bundle bundle) {
		if (bundle == null) return;
		bundle.putLong(KEY_ITEM_ID, this.itemId);
		bundle.putInt(KEY_TOP, this.top);
	}

	public static ScrollState from (final ListView lv) {
		final int index = lv.getFirstVisiblePosition();
		final View v = lv.getChildAt(0);
		final int top = (v == null) ? 0 : v.getTop();

		final long itemId = lv.getAdapter().getItemId(index);
		if (itemId <= 0) return null;

		return new ScrollState(itemId, top);
	}

	public static ScrollState fromBundle (final Bundle bundle) {
		if (bundle == null) return null;
		if (bundle.containsKey(KEY_ITEM_ID) && bundle.containsKey(KEY_TOP)) {
			final long itemId = bundle.getLong(KEY_ITEM_ID);
			final int top = bundle.getInt(KEY_TOP);
			return new ScrollState(itemId, top);
		}
		return null;
	}

}
