package net.sparktank.morrigan.android.tasks;

import java.io.IOException;
import java.net.ConnectException;

import net.sparktank.morrigan.android.helper.HttpHelper;
import net.sparktank.morrigan.android.model.MlistReference;
import net.sparktank.morrigan.android.model.MlistState;
import net.sparktank.morrigan.android.model.MlistStateChangeListener;
import net.sparktank.morrigan.android.model.impl.MlistStateXmlImpl;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

public class GetMlistTask extends AsyncTask<Void, Void, MlistState> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Activity activity;
	private final MlistReference mlistReference;
	private final MlistStateChangeListener changedListener;
	
	private Exception exception;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetMlistTask (Activity activity, MlistReference mlistReference, MlistStateChangeListener changedListener) {
		this.activity = activity;
		this.mlistReference = mlistReference;
		this.changedListener = changedListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// In UI thread:
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.activity.setProgressBarIndeterminateVisibility(true);
	}
	
	// In background thread:
	@Override
	protected MlistState doInBackground(Void... params) {
		String url = this.mlistReference.getBaseUrl();
		
		try {
			String resp = HttpHelper.getUrlContent(url);
			MlistState state = new MlistStateXmlImpl(resp);
			return state;
		}
		catch (ConnectException e) {
			this.exception = e;
			return null;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
	}
	
	// In UI thread:
	@Override
	protected void onPostExecute(MlistState result) {
		super.onPostExecute(result);
		
		if (this.exception != null) { // TODO handle this better.
			Toast.makeText(this.activity, this.exception.getMessage(), Toast.LENGTH_LONG).show();
		}
		
		if (this.changedListener != null) this.changedListener.onMlistStateChange(result);
		
		this.activity.setProgressBarIndeterminateVisibility(false);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
