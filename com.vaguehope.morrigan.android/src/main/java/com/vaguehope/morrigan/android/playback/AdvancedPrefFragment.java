package com.vaguehope.morrigan.android.playback;

import java.io.File;

import com.vaguehope.morrigan.android.helper.DialogHelper;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.helper.LogcatHelper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

public class AdvancedPrefFragment extends PreferenceFragment {

	private static final LogWrapper LOG = new LogWrapper("ADVPREF");

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
		addEntries();
	}

	private void addEntries () {
		addDumpLogPref();
	}

	private void addDumpLogPref () {
		final Preference pref = new Preference(getActivity());
		pref.setTitle("Dump log"); //ES
		pref.setSummary("Write app log to /sdcard/morrigan-<time>.txt"); //ES
		pref.setOnPreferenceClickListener(this.dumpLogsClickListener);
		getPreferenceScreen().addPreference(pref);
	}

	private final OnPreferenceClickListener dumpLogsClickListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick (final Preference preference) {
			dumpLog();
			return true;
		}
	};

	protected void dumpLog () {
		try {
			final File file = new File(Environment.getExternalStorageDirectory(), "morrigan-" + System.currentTimeMillis() + ".txt");
			new DumpLog(getActivity(), file).execute();
		}
		catch (final Exception e) { // NOSONAR show user all errors.
			LOG.e("Failed to dump log.", e);
			DialogHelper.alert(getActivity(), e);
		}
	}

	private static class DumpLog extends AsyncTask<Void, Void, Exception> {

		private final Context context;
		private final File file;

		private ProgressDialog dialog;

		public DumpLog (final Context context, final File file) {
			this.context = context;
			this.file = file;
		}

		@Override
		protected void onPreExecute () {
			this.dialog = ProgressDialog.show(this.context, "Log", "Saving...", true); //ES
		}

		@Override
		protected Exception doInBackground (final Void... params) {
			try {
				LogcatHelper.dumpLog(this.file);
				return null;
			}
			catch (final Exception e) { // NOSONAR show user all errors.
				return e;
			}
		}

		@Override
		protected void onPostExecute (final Exception result) {
			this.dialog.dismiss();
			if (result == null) {
				DialogHelper.alert(this.context, "Log written to:\n" + this.file.getAbsolutePath()); //ES
			}
			else {
				LOG.e("Failed to dump read later.", result);
				DialogHelper.alert(this.context, result);
			}
		}

	}

}
