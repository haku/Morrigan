/*
 * Copyright 2010 Alex Hutter
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

import com.vaguehope.morrigan.android.Constants;
import com.vaguehope.morrigan.android.helper.HttpHelper.HttpCreds;
import com.vaguehope.morrigan.android.model.MlistStateList;
import com.vaguehope.morrigan.android.model.MlistStateListChangeListener;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.modelimpl.MlistStateListImpl;

import android.app.Activity;

public class GetMlistsTask extends AbstractTask<MlistStateList> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final ServerReference serverReference;
	private final MlistStateListChangeListener changedListener;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetMlistsTask (Activity activity, ServerReference serverReference, MlistStateListChangeListener changedListener) {
		super(activity);
		this.serverReference = serverReference;
		this.changedListener = changedListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected String getUrl () {
		return this.serverReference.getBaseUrl().concat(Constants.CONTEXT_MLISTS);
	}
	
	@Override
	protected HttpCreds getCreds () {
		return this.serverReference;
	}
	
	// In background thread:
	@Override
	protected MlistStateList parseStream (InputStream is) throws IOException, SAXException {
		return new MlistStateListImpl(is, GetMlistsTask.this.serverReference);
	}
	
	// In UI thread:
	@Override
	protected void onSuccess (MlistStateList result) {
		if (this.changedListener != null) this.changedListener.onMlistsChange(result);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
