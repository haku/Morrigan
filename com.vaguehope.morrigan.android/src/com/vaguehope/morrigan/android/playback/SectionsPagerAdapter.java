package com.vaguehope.morrigan.android.playback;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class SectionsPagerAdapter extends FragmentPagerAdapter {

	private final PlaybackActivity host;

	public SectionsPagerAdapter (final FragmentManager fm, final PlaybackActivity host) {
		super(fm);
		this.host = host;
	}

	@Override
	public Fragment getItem (final int argPosition) {
		switch (argPosition) {
			case 0:
				return new PlayerFragment();
			case 1:
				return new LibraryFragment();
			default:
				throw new IllegalArgumentException("Unknown page.");
		}
	}

	@Override
	public int getCount () {
		return 2;
	}

	@Override
	public CharSequence getPageTitle (final int argPosition) {
		switch (argPosition) {
			case 0:
				return "Player";
			case 1:
				return "Library";
			default:
				throw new IllegalArgumentException("Unknown page.");
		}
	}

	@Override
	public float getPageWidth (final int position) {
		return 1;
	}

}
