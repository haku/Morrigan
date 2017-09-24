package com.vaguehope.morrigan.android.playback;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.LogWrapper;

public class LibraryFragment extends Fragment {

	private static final LogWrapper LOG = new LogWrapper("LF");

	private PlaybackActivity hostActivity;

	@Override
	public View onCreateView (final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		this.hostActivity = (PlaybackActivity) getActivity();

		final View rootView = inflater.inflate(R.layout.playback_library, container, false);
		wireGui(rootView);
		return rootView;
	}

	private Playbacker getPlaybacker () {
		return this.hostActivity.getPlaybacker();
	}

	private void wireGui (final View rootView) {
	}

}
