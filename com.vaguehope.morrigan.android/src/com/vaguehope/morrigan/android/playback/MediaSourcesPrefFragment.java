package com.vaguehope.morrigan.android.playback;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.support.v4.provider.DocumentFile;
import android.view.View;
import android.view.View.OnLongClickListener;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.ActivityResultTracker;
import com.vaguehope.morrigan.android.helper.ActivityResultTracker.ActivityResultCallback;
import com.vaguehope.morrigan.android.helper.DialogHelper;
import com.vaguehope.morrigan.android.helper.DialogHelper.Listener;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.helper.StringHelper;

public class MediaSourcesPrefFragment extends MnPreferenceFragment {

	protected static final LogWrapper LOG = new LogWrapper("MSP");

	private final ActivityResultTracker activityResultTracker = new ActivityResultTracker(PlaybackCodes.MEDIA_SOURCE_PREF_REQUEST_CODE);

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
	}

	@Override
	public void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
		this.activityResultTracker.onActivityResult(requestCode, resultCode, data);
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

		final Preference rescanDbs = new Preference(getActivity());
		rescanDbs.setTitle("Rescan DBs...");
		rescanDbs.setIcon(R.drawable.search);
		rescanDbs.setOnPreferenceClickListener(this.rescanDbsClickListener);
		getPreferenceScreen().addPreference(rescanDbs);

		for (final DbMetadata db : getMediaDb().getDbs()) {
			final PreferenceCategory group = new PreferenceCategory(getActivity());
			group.setIcon(R.drawable.db);
			group.setTitle(db.getName());
			getPreferenceScreen().addPreference(group);

			for (final Uri source : db.getSources()) {
				group.addPreference(new ExistingSourcePreference(getActivity(), db, source));
			}
			group.addPreference(new AddSourceClickListener(getActivity(), db));
			group.addPreference(new RenameDbClickListener(getActivity(), db));
		}
	}

	private final OnPreferenceClickListener newDbClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			promptCreateNewDb();
			return true;
		}
	};

	private final OnPreferenceClickListener rescanDbsClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			getActivity().startService(new Intent(getActivity(), RescanDbsService.class));
			return true;
		}
	};

	protected void promptCreateNewDb () {
		DialogHelper.askString(getActivity(), "Name for new DB:", new Listener<String>() {
			@Override
			public void onAnswer (final String name) {
				if (StringHelper.notEmpty(name)) {
					getMediaDb().newDb(name);
					refreshList();
				}
			}
		});
	}

	private class AddSourceClickListener extends Preference implements OnPreferenceClickListener {

		private final DbMetadata db;

		public AddSourceClickListener (final Context context, final DbMetadata db) {
			super(context);
			this.db = db;
			setTitle("Add Source...");
			setIcon(R.drawable.plus);
			setOnPreferenceClickListener(this);
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

			startActivityForResult(intent, MediaSourcesPrefFragment.this.activityResultTracker.registerCallback(new ActivityResultCallback() {
				@Override
				public void onActivityResult (final int resultCode, final Intent data) {
					if (data == null) return;

					final Uri uri = data.getData();
					if (uri == null) return;

					final DocumentFile file = DocumentFile.fromTreeUri(getActivity(), uri);
					if (!file.isDirectory()) {
						DialogHelper.alert(getActivity(), "Not a directory: " + file.getName());
						return;
					}

					LOG.i("Adding source '%s' to DB %s.", uri, AddSourceClickListener.this.db);
					getMediaDb().updateDb(AddSourceClickListener.this.db.withSource(uri));
					refreshList();
				}
			}));
			return true;
		}

	}

	private class RenameDbClickListener extends Preference implements OnPreferenceClickListener {

		private final DbMetadata db;

		public RenameDbClickListener (final Context context, final DbMetadata db) {
			super(context);
			this.db = db;
			setTitle("Rename DB...");
			setIcon(android.R.drawable.ic_menu_edit);
			setOnPreferenceClickListener(this);
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			DialogHelper.askString(getActivity(), "Rename " + this.db.getName() + ":", this.db.getName(), new Listener<String>() {
				@Override
				public void onAnswer (final String name) {
					if (StringHelper.notEmpty(name)) {
						getMediaDb().updateDb(RenameDbClickListener.this.db.withName(name));
						refreshList();
					}
				}
			});
			return true;
		}

	}

	private class ExistingSourcePreference extends Preference implements OnLongClickListener {

		private final DbMetadata db;
		private final Uri source;

		public ExistingSourcePreference (final Context context, final DbMetadata db, final Uri source) {
			super(context);
			this.db = db;
			this.source = source;

			final DocumentFile file = DocumentFile.fromTreeUri(context, source);
			setTitle(file.getName());
			setSummary(source.toString());
			setIcon(R.drawable.circledot);
		}

		@Override
		public boolean onLongClick (final View v) {
			DialogHelper.askYesNo(getContext(), "Remove source " + getTitle() + "?\n\n" + this.source, new Runnable() {
				@Override
				public void run () {
					LOG.i("Removing source '%s' from DB %s.", ExistingSourcePreference.this.source, ExistingSourcePreference.this.db);
					getMediaDb().updateDb(ExistingSourcePreference.this.db.withoutSource(ExistingSourcePreference.this.source));
					refreshList();
				}
			});
			return true;
		}
	}

}
