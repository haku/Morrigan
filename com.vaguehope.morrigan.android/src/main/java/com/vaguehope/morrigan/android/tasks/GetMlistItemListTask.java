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
import java.net.URLEncoder;

import org.xml.sax.SAXException;

import android.app.Activity;

import com.vaguehope.morrigan.android.C;
import com.vaguehope.morrigan.android.helper.HttpHelper.HttpCreds;
import com.vaguehope.morrigan.android.model.MlistItemList;
import com.vaguehope.morrigan.android.model.MlistItemListChangeListener;
import com.vaguehope.morrigan.android.model.MlistReference;
import com.vaguehope.morrigan.android.modelimpl.MlistItemListImpl;

public class GetMlistItemListTask extends AbstractTask<MlistItemList> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final MlistReference mlistReference;
	private final MlistItemListChangeListener changedListener;
	protected final String query;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public GetMlistItemListTask (Activity activity, MlistReference mlistReference, MlistItemListChangeListener changedListener) {
		this(activity, mlistReference, changedListener, null);
	}

	public GetMlistItemListTask (Activity activity, MlistReference mlistReference, MlistItemListChangeListener changedListener, String query) {
		super(activity);
		this.mlistReference = mlistReference;
		this.changedListener = changedListener;
		this.query = query;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected String getProgressMsg () {
		return "Fetching media items...";
	}

	@Override
	protected HttpCreds getCreds () {
		return this.mlistReference.getServerReference();
	}

	@Override
	protected String getUrl () {
		String url = this.mlistReference.getBaseUrl();

		if (this.query != null) {
			String encodedQuery = URLEncoder.encode(this.query);
			url = url.concat(C.CONTEXT_MLIST_QUERY + "/" + encodedQuery);
		}
		else {
			url = url.concat(C.CONTEXT_MLIST_ITEMS);
		}

		return url;
	}

	@Override
	protected MlistItemList parseStream (InputStream is) throws IOException, SAXException {
		return new MlistItemListImpl(is, GetMlistItemListTask.this.query);
	}

	// In UI thread:
	@Override
	protected void onSuccess (MlistItemList result, Exception exception) {
		if (this.changedListener != null) this.changedListener.onMlistItemListChange(result, exception);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
