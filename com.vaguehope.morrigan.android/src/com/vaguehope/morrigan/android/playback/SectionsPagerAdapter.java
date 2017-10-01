package com.vaguehope.morrigan.android.playback;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;

import com.vaguehope.morrigan.android.helper.StringHelper;

public class SectionsPagerAdapter extends FragmentPagerAdapter {

	protected static final String ARG_FRAGMENT_POSITION = "fragment_position";

	private final SparseArray<String> pageTitles = new SparseArray<String>();

	public SectionsPagerAdapter (final FragmentManager fm) {
		super(fm);
	}

	public void setPageTitle(final int position, final String title) {
		this.pageTitles.put(position, title);
		notifyDataSetChanged();
	}

	@Override
	public Fragment getItem (final int argPosition) {
		final Fragment fragment;
		final Bundle args = new Bundle();
		args.putInt(SectionsPagerAdapter.ARG_FRAGMENT_POSITION, argPosition);

		switch (argPosition) {
			case 0:
				fragment = new PlayerFragment();
				break;
			case 1:
				fragment = new LibraryFragment();
				break;
			default:
				throw new IllegalArgumentException("Unknown page.");
		}

		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public int getCount () {
		return 2;
	}

	@Override
	public CharSequence getPageTitle (final int argPosition) {
		final String title = this.pageTitles.get(argPosition);
		if (StringHelper.notEmpty(title)) return title;

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
