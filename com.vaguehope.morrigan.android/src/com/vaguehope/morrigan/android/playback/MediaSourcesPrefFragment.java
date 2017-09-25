package com.vaguehope.morrigan.android.playback;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.DialogHelper;
import com.vaguehope.morrigan.android.helper.DialogHelper.Listener;
import com.vaguehope.morrigan.android.helper.StringHelper;

public class MediaSourcesPrefFragment extends MnPreferenceFragment {

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
	}

	@Override
	protected void onMediaDbBound () {
		refreshList();
	}

	private void refreshList () {
		getPreferenceScreen().removeAll();

		final Preference newDb = new Preference(getActivity());
		newDb.setTitle("New Media DB...");
		newDb.setIcon(R.drawable.plus);
		newDb.setOnPreferenceClickListener(this.newDbClickListener);
		getPreferenceScreen().addPreference(newDb);

		for (final DbMetadata db : getMediaDb().getDbs()) {
			final PreferenceCategory group = new PreferenceCategory(getActivity());
			group.setIcon(R.drawable.db);
			group.setTitle(db.getName());
			getPreferenceScreen().addPreference(group);

			for (final Uri source : db.getSources()) {
				group.addPreference(new ExistingSourcePreference(getActivity(), source));
			}

			final Preference addSource = new Preference(getActivity());
			addSource.setTitle("Add Source...");
			addSource.setIcon(R.drawable.plus);
			addSource.setOnPreferenceClickListener(new AddSourceClickListener(db));
			group.addPreference(addSource);
		}
	}

	private final OnPreferenceClickListener newDbClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			promptCreateNewDb();
			return true;
		}
	};

	protected void promptCreateNewDb () {
		DialogHelper.askString(getActivity(), "Name for new DB:", new Listener<String>() {
			@Override
			public void onAnswer (final String name) {
				if (StringHelper.notEmpty(name)) {
					getMediaDb().newDb(name);
				}
			}
		});
	}

	private class AddSourceClickListener implements OnPreferenceClickListener {

		private final DbMetadata db;

		public AddSourceClickListener (final DbMetadata db) {
			this.db = db;
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			DialogHelper.alert(getActivity(), "TODO: Add source to " + this.db.getName());
			return true;
		}

	}

	private class ExistingSourcePreference extends Preference implements OnLongClickListener {

		private final Uri source;

		public ExistingSourcePreference (final Context context, final Uri source) {
			super(context);
			this.source = source;
			setTitle(source.toString());
			setIcon(R.drawable.circledot);
		}

		@Override
		public boolean onLongClick (final View v) {
			DialogHelper.alert(getActivity(), "TODO: Prompt to remove source " + this.source);
			return true;
		}

	}

}
