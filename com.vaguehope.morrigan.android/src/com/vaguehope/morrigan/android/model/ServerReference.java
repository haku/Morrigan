package com.vaguehope.morrigan.android.model;

import com.vaguehope.morrigan.android.helper.HttpHelper.HttpCreds;
import com.vaguehope.morrigan.android.helper.Titleable;

public interface ServerReference extends Artifact, HttpCreds, Titleable {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public String getName ();

	public String getBaseUrl ();

	@Override
	public String getPass ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
