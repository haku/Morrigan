package net.sparktank.morrigan.android.tasks;

import net.sparktank.morrigan.android.model.MlistItem;
import net.sparktank.morrigan.android.model.ServerReference;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class DownloadMediaTask extends AsyncTask<MlistItem, Integer, String> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final ServerReference serverReference;

	private ProgressDialog dialog;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public DownloadMediaTask (Context context, ServerReference serverReference) {
		this.context = context;
		this.serverReference = serverReference;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public Context getContext () {
		return this.context;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static int PRGMAX = 10000;
	
	@Override
	protected void onPreExecute () {
		ProgressDialog progressDialog = new ProgressDialog(this.context);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setCancelable(false);
		progressDialog.setIndeterminate(false);
		progressDialog.setMax(PRGMAX);
		progressDialog.setTitle("Downloading...");
//		progressDialog.setProgressNumberFormat(null); // Not available 'till API 11 (3.x).
		progressDialog.show();
		this.dialog = progressDialog;
	}
	
	@Override
	protected String doInBackground (MlistItem... items) {
		if (items.length < 1) throw new IllegalArgumentException("No items desu~");
		
		final int pPerItem = (int) (PRGMAX / (float)items.length);
		
		for (MlistItem item : items) {
			String url = this.serverReference.getBaseUrl() + item.getRelativeUrl();
			
			for (int i = 0; i < 100; i++) {
				// TODO actually download file here.
				publishProgress(Integer.valueOf(0), Integer.valueOf((int) (pPerItem / 100f)));
				try { Thread.sleep(100); } catch (InterruptedException e) { /* Unused */ }
			}
			
			publishProgress(Integer.valueOf(pPerItem));
		}
		
		return "Pseudo success desu~";
	}
	
	@Override
	protected void onProgressUpdate (Integer... values) {
		if (values.length >= 1 && values[0] != null && values[0].intValue() > 0) {
			this.dialog.incrementProgressBy(values[0].intValue());
		}
		if (values.length >= 2 && values[1] != null && values[1].intValue() > 0) {
			this.dialog.incrementSecondaryProgressBy(values[1].intValue());
		}
	}
	
	@Override
	protected void onPostExecute (String result) {
		if (this.dialog != null) this.dialog.dismiss(); // This will fail if the screen is rotated while we are fetching.
		Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
