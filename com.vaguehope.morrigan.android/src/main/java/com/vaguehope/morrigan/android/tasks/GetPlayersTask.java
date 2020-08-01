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

import android.app.Activity;
import android.content.Context;

import com.vaguehope.morrigan.android.C;
import com.vaguehope.morrigan.android.helper.HttpHelper.HttpCreds;
import com.vaguehope.morrigan.android.model.PlayerStateList;
import com.vaguehope.morrigan.android.model.PlayerStateListChangeListener;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.modelimpl.PlayerStateListImpl;

public class GetPlayersTask extends AbstractTask<PlayerStateList> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected final ServerReference serverReference;
	private final PlayerStateListChangeListener changedListener;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public GetPlayersTask (Context context, ServerReference serverReference, PlayerStateListChangeListener changedListener) {
		super(context);
		this.serverReference = serverReference;
		this.changedListener = changedListener;
	}

	public GetPlayersTask (Activity activity, ServerReference serverReference, PlayerStateListChangeListener changedListener) {
		super(activity);
		this.serverReference = serverReference;
		this.changedListener = changedListener;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected String getProgressMsg () {
		return getActivity() == null ? "Fetching player data..." : null; // Only return msg if activity is null.
	}

	@Override
	protected HttpCreds getCreds () {
		return this.serverReference;
	}

	@Override
	protected String getUrl () {
		return this.serverReference.getBaseUrl().concat(C.CONTEXT_PLAYERS);
	}

	// In background thread:
	@Override
	protected PlayerStateList parseStream (InputStream is) throws IOException, SAXException {
		return new PlayerStateListImpl(is, this.serverReference);
	}

	// In UI thread:
	@Override
	protected void onSuccess (PlayerStateList result, Exception exception) {
		if (this.changedListener != null) this.changedListener.onPlayersChange(result, exception);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
