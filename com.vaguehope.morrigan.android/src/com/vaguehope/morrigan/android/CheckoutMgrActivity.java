package com.vaguehope.morrigan.android;

import android.app.Activity;
import android.os.Bundle;

import com.vaguehope.morrigan.android.state.ConfigDb;

public class CheckoutMgrActivity extends Activity  {

	private ConfigDb configDb;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.configDb = new ConfigDb(this);
		setContentView(R.layout.checkoutmgr);
	}

}
