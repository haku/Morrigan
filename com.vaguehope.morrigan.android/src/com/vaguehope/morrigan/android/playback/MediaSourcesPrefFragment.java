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

		final Preference rescanLibraries = new Preference(getActivity());
		rescanLibraries.setTitle("Rescan Libraries...");
		rescanLibraries.setIcon(R.drawable.search);
		rescanLibraries.setOnPreferenceClickListener(this.rescanLibrariesClickListener);
		getPreferenceScreen().addPreference(rescanLibraries);

		final Preference newLibrary = new Preference(getActivity());
		newLibrary.setTitle("New Media Library...");
		newLibrary.setIcon(R.drawable.plus);
		newLibrary.setOnPreferenceClickListener(this.newLibraryClickListener);
		getPreferenceScreen().addPreference(newLibrary);

		for (final LibraryMetadata library : getMediaDb().getLibraries()) {
			final PreferenceCategory group = new PreferenceCategory(getActivity());
			group.setIcon(R.drawable.db);
			group.setTitle(library.getName());
			getPreferenceScreen().addPreference(group);

			for (final Uri source : library.getSources()) {
				group.addPreference(new ExistingSourcePreference(getActivity(), library, source));
			}
			group.addPreference(new AddSourceClickListener(getActivity(), library));
			group.addPreference(new RenameLibraryClickListener(getActivity(), library));
		}
	}

	private final OnPreferenceClickListener newLibraryClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			promptCreateNewLibrary();
			return true;
		}
	};

	private final OnPreferenceClickListener rescanLibrariesClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			getActivity().startService(new Intent(getActivity(), RescanLibrariesService.class));
			return true;
		}
	};

	protected void promptCreateNewLibrary () {
		DialogHelper.askString(getActivity(), "Name for new library:", new Listener<String>() {
			@Override
			public void onAnswer (final String name) {
				if (StringHelper.notEmpty(name)) {
					getMediaDb().newLibrary(name);
					refreshList();
				}
			}
		});
	}

	private class AddSourceClickListener extends Preference implements OnPreferenceClickListener {

		private final LibraryMetadata libraryMetadata;

		public AddSourceClickListener (final Context context, final LibraryMetadata libraryMetadata) {
			super(context);
			this.libraryMetadata = libraryMetadata;
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

					LOG.i("Adding source '%s' to library %s.", uri, AddSourceClickListener.this.libraryMetadata);
					getMediaDb().updateLibrary(AddSourceClickListener.this.libraryMetadata.withSource(uri));
					refreshList();
				}
			}));
			return true;
		}

	}

	private class RenameLibraryClickListener extends Preference implements OnPreferenceClickListener {

		private final LibraryMetadata libraryMetadata;

		public RenameLibraryClickListener (final Context context, final LibraryMetadata libraryMetadata) {
			super(context);
			this.libraryMetadata = libraryMetadata;
			setTitle("Rename Library...");
			setIcon(android.R.drawable.ic_menu_edit);
			setOnPreferenceClickListener(this);
		}

		@Override
		public boolean onPreferenceClick (final Preference preference) {
			DialogHelper.askString(getActivity(), "Rename " + this.libraryMetadata.getName() + ":", this.libraryMetadata.getName(), new Listener<String>() {
				@Override
				public void onAnswer (final String name) {
					if (StringHelper.notEmpty(name)) {
						getMediaDb().updateLibrary(RenameLibraryClickListener.this.libraryMetadata.withName(name));
						refreshList();
					}
				}
			});
			return true;
		}

	}

	private class ExistingSourcePreference extends Preference implements OnLongClickListener {

		private final LibraryMetadata libraryMetadata;
		private final Uri source;

		public ExistingSourcePreference (final Context context, final LibraryMetadata libraryMetadata, final Uri source) {
			super(context);
			this.libraryMetadata = libraryMetadata;
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
					LOG.i("Removing source '%s' from library %s.", ExistingSourcePreference.this.source, ExistingSourcePreference.this.libraryMetadata);
					getMediaDb().updateLibrary(ExistingSourcePreference.this.libraryMetadata.withoutSource(ExistingSourcePreference.this.source));
					refreshList();
				}
			});
			return true;
		}
	}

}
