package com.vaguehope.morrigan.android.playback;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

public class MnPreferenceFragment extends PreferenceFragment {

	@Override
	public void onStart () {
		super.onStart();
		getPrefActivity().addOnMediaDbBound(this.onMediaDbBound);
	}

	@Override
	public void onStop () {
		getPrefActivity().removeOnMediaDbBound(this.onMediaDbBound);
		super.onStop();
	}

	protected MnPreferenceActivity getPrefActivity () {
		return (MnPreferenceActivity) getActivity();
	}

	protected MediaDb getMediaDb () {
		return getPrefActivity().getMediaDb();
	}

	private final Runnable onMediaDbBound = new Runnable() {
		@Override
		public void run () {
			onMediaDbBound();
		}
	};

	protected void onMediaDbBound () {
		// Noop.
	}

	@Override
	public void onViewStateRestored (final Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);

		final View view = getView();
		if (view == null) throw new IllegalStateException();

		final View rawView = view.findViewById(android.R.id.list);
		if (rawView == null) throw new IllegalStateException();
		if (!(rawView instanceof ListView)) throw new IllegalStateException();

		((ListView) rawView).setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick (final AdapterView<?> parent, final View view, final int position, final long id) {
				final Object obj = ((ListView) parent).getAdapter().getItem(position);
				if (obj != null && obj instanceof View.OnLongClickListener) {
					final View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
					return longListener.onLongClick(view);
				}
				return false;
			}
		});
	}

}
