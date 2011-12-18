/*
 * Copyright 2010 Fae Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.vaguehope.morrigan.android.tasks;

import java.io.IOException;
import java.io.InputStream;


import org.xml.sax.SAXException;

import com.vaguehope.morrigan.android.helper.HttpHelper.HttpCreds;
import com.vaguehope.morrigan.android.model.MlistReference;
import com.vaguehope.morrigan.android.model.MlistState;
import com.vaguehope.morrigan.android.model.MlistStateChangeListener;
import com.vaguehope.morrigan.android.model.impl.MlistStateXmlImpl;

import android.app.Activity;

public class GetMlistTask extends AbstractTask<MlistState> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final MlistReference mlistReference;
	private final MlistStateChangeListener changedListener;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetMlistTask (Activity activity, MlistReference mlistReference, MlistStateChangeListener changedListener) {
		super(activity);
		this.mlistReference = mlistReference;
		this.changedListener = changedListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected String getUrl () {
		return this.mlistReference.getBaseUrl();
	}
	
	@Override
	protected HttpCreds getCreds () {
		return this.mlistReference.getServerReference();
	}
	
	// In background thread:
	@Override
	protected MlistState parseStream (InputStream is) throws IOException, SAXException {
		String data = parseStreamToString(is);
		return new MlistStateXmlImpl(data, this.mlistReference);
	}
	
	// In UI thread:
	@Override
	protected void onSuccess (MlistState result) {
		if (this.changedListener != null) this.changedListener.onMlistStateChange(result);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
