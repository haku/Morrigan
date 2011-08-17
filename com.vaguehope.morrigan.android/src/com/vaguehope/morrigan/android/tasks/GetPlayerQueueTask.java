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

import com.vaguehope.morrigan.android.Constants;
import com.vaguehope.morrigan.android.model.PlayerQueue;
import com.vaguehope.morrigan.android.model.PlayerQueueChangeListener;
import com.vaguehope.morrigan.android.model.PlayerReference;
import com.vaguehope.morrigan.android.model.impl.PlayerQueueImpl;

import android.app.Activity;

public class GetPlayerQueueTask extends AbstractTask<PlayerQueue> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final PlayerReference playerReference;
	private final PlayerQueueChangeListener changeListener;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetPlayerQueueTask (Activity activity, PlayerReference playerReference, PlayerQueueChangeListener changeListener) {
		super(activity);
		this.playerReference = playerReference;
		this.changeListener = changeListener;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected String getUrl () {
		return this.playerReference.getBaseUrl().concat(Constants.CONTEXT_PLAYER_QUEUE);
	}
	
	// In background thread:
	@Override
	protected PlayerQueue parseStream (InputStream is) throws IOException, SAXException {
		return new PlayerQueueImpl(is, GetPlayerQueueTask.this.playerReference);
	}
	
	// In UI thread:
	@Override
	protected void onSuccess (PlayerQueue result) {
		if (this.changeListener != null) this.changeListener.onPlayerQueueChange(result);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}