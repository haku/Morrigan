package com.vaguehope.morrigan.android.tasks;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.widget.Toast;

public class BulkRunner<T> extends AsyncTask<Void, Integer, String> implements OnClickListener {

	private final Context context;
	private final List<AbstractTask<T>> tasks;
	private ProgressDialog progressDialog;
	private final AtomicBoolean aborted = new AtomicBoolean(false);

	public BulkRunner (final Context context, final List<AbstractTask<T>> tasks) {
		this.context = context;
		this.tasks = tasks;
	}

	@Override
	protected void onPreExecute () {
		final ProgressDialog dlg = new ProgressDialog(this.context);
		dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dlg.setIndeterminate(false);
		dlg.setMax(this.tasks.size());
		dlg.setCancelable(true);
		dlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", this);
		dlg.show();
		this.progressDialog = dlg;
	}

	@Override
	public void onClick (final DialogInterface dialog, final int which) {
		if (which == DialogInterface.BUTTON_NEGATIVE) {
			this.aborted.set(true);
		}
	}

	@Override
	protected String doInBackground (final Void... params) {
		final AtomicInteger counter = new AtomicInteger(0);
		final ExecutorService exe = Executors.newSingleThreadExecutor();
		try {
			for (final AbstractTask<T> task : this.tasks) {
				if (this.aborted.get()) return null;
				task.executeOnExecutor(exe);
				task.get();
				publishProgress(Integer.valueOf(counter.incrementAndGet()));
			}
		}
		catch (final Exception e) {
			return e.getMessage();
		}
		finally {
			exe.shutdown();
		}
		return null;
	}

	@Override
	protected void onProgressUpdate (final Integer... values) {
		if (values.length >= 1 && values[0] != null && values[0].intValue() > 0) {
			this.progressDialog.setProgress(values[0].intValue());
		}
	}

	@Override
	protected void onPostExecute (final String msg) {
		if (msg != null) Toast.makeText(this.context, msg, Toast.LENGTH_SHORT).show();
		if (this.progressDialog != null) this.progressDialog.dismiss(); // This will fail if the screen is rotated while we are fetching.
	}

}
