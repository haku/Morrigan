package net.sparktank.morrigan.android.tasks;

import java.io.IOException;

import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.model.ServerReference;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

public class PlaypauseTask extends AsyncTask<Void, Void, Boolean> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Context context;
	private final ServerReference serverReference;
	
	private ProgressDialog dialog;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public PlaypauseTask (Context context, ServerReference serverReference) {
		this.context = context;
		this.serverReference = serverReference;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// In UI thread:
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.dialog = ProgressDialog.show(this.context, "Play / Pause", "Please wait...", true);
	}
	
	// In background thread:
	@Override
	protected Boolean doInBackground(Void... params) {
		String url = this.serverReference.getBaseUrl() + "/player/0/playpause";
		
		try {
			// TODO parse response?
			HttpHelper.getUrlContent(url);
			
		}
		catch (IOException e) {
			e.printStackTrace();
			return Boolean.FALSE;
		}
		
		return Boolean.TRUE;
	}
	
	// In UI thread:
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		
		Toast.makeText(this.context, "Playpause result: " + result, Toast.LENGTH_LONG).show();
		
		// FIXME This will fail if the screen is rotated while we are fetching.
		this.dialog.dismiss();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
