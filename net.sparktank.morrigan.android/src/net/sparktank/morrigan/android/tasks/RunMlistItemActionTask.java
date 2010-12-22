package net.sparktank.morrigan.android.tasks;

import java.io.IOException;
import java.net.ConnectException;

import net.sparktank.morrigan.android.TempConstants;
import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.model.MlistItem;
import net.sparktank.morrigan.android.model.ServerReference;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

public class RunMlistItemActionTask extends AsyncTask<Void, Void, String> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public enum MlistItemCommand {
		PLAY(0), QUEUE(1);
		
		private int n;
		
		private MlistItemCommand (int n) {
			this.n = n;
		}
		
		public int getN() {
			return this.n;
		}
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Activity activity;
	private final ServerReference serverReference;
	private final MlistItem mlistItem;
	private final MlistItemCommand cmd;
	
	private Exception exception;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public RunMlistItemActionTask (Activity activity, ServerReference serverReference, MlistItem mlistItem, MlistItemCommand cmd) {
		this.activity = activity;
		this.serverReference = serverReference;
		this.mlistItem = mlistItem;
		this.cmd = cmd;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private ProgressDialog dialog;
	
	// In UI thread:
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.dialog = ProgressDialog.show(this.activity, null, "Please wait...", true);
	}
	
	// In background thread:
	@Override
	protected String doInBackground(Void... params) {
		String url = this.serverReference.getBaseUrl() + this.mlistItem.getRelativeUrl();
		
		String encodedData = "action=";
		switch (this.cmd) {
			case PLAY:
				encodedData = encodedData.concat("play");
				break;
				
			case QUEUE:
				encodedData = encodedData.concat("queue");
				break;
				
			default: throw new IllegalArgumentException();
		}
		
		encodedData = encodedData.concat("&playerid=" + TempConstants.PLAYERID);
		
		try {
			String resp = HttpHelper.getUrlContent(url, "POST", encodedData, "application/x-www-form-urlencoded");
			return resp;
		}
		catch (ConnectException e) {
			this.exception = e;
			return null;
		} catch (IOException e) {
			this.exception = e;
			return null;
		}
	}
	
	// In UI thread:
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		
		if (this.exception != null) { // TODO handle this better.
			Toast.makeText(this.activity, this.exception.getMessage(), Toast.LENGTH_LONG).show();
		}
		else {
			Toast.makeText(this.activity, result, Toast.LENGTH_LONG).show();
		}
		
		this.dialog.dismiss(); // This will fail if the screen is rotated while we are fetching.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
