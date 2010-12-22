package net.sparktank.morrigan.android.tasks;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URLEncoder;

import net.sparktank.morrigan.android.Constants;
import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.model.MlistItemList;
import net.sparktank.morrigan.android.model.MlistItemListChangeListener;
import net.sparktank.morrigan.android.model.MlistReference;
import net.sparktank.morrigan.android.model.impl.MlistItemListImpl;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

public class GetMlistItemListTask extends AsyncTask<Void, Void, MlistItemList> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Activity activity;
	private final MlistReference mlistReference;
	private final MlistItemListChangeListener changedListener;
	private final String query;
	
	private Exception exception;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetMlistItemListTask (Activity activity, MlistReference mlistReference, MlistItemListChangeListener changedListener) {
		this(activity, mlistReference, changedListener, null);
	}
	
	public GetMlistItemListTask (Activity activity, MlistReference mlistReference, MlistItemListChangeListener changedListener, String query) {
		this.activity = activity;
		this.mlistReference = mlistReference;
		this.changedListener = changedListener;
		this.query = query;
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
	protected MlistItemList doInBackground(Void... params) {
		String url = this.mlistReference.getBaseUrl();
		
		if (this.query != null) {
			String encodedQuery = URLEncoder.encode(this.query);
			url = url.concat(Constants.CONTEXT_MLIST_QUERY + "/" + encodedQuery);
		}
		else {
			url = url.concat(Constants.CONTEXT_MLIST_ITEMS);
		}
		
		try {
			String resp = HttpHelper.getUrlContent(url);
			MlistItemList list = new MlistItemListImpl(resp);
			return list;
		}
		catch (ConnectException e) {
			this.exception = e;
			return null;
		} catch (IOException e) {
			this.exception = e;
			return null;
		} catch (SAXException e) {
			this.exception = e;
			return null;
		}
	}
	
	// In UI thread:
	@Override
	protected void onPostExecute(MlistItemList result) {
		super.onPostExecute(result);
		
		if (this.exception != null) { // TODO handle this better.
			Toast.makeText(this.activity, this.exception.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		if (this.changedListener != null) this.changedListener.onMlistItemListChange(result);
		
		this.dialog.dismiss(); // This will fail if the screen is rotated while we are fetching.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
