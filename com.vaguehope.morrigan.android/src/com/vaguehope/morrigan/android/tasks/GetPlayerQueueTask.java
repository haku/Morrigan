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
import com.vaguehope.morrigan.android.helper.HttpHelper.HttpCreds;
import com.vaguehope.morrigan.android.model.Artifact;
import com.vaguehope.morrigan.android.model.PlayerQueue;
import com.vaguehope.morrigan.android.model.PlayerQueueChangeListener;
import com.vaguehope.morrigan.android.model.PlayerReference;
import com.vaguehope.morrigan.android.modelimpl.PlayerQueueImpl;

import android.app.Activity;

public class GetPlayerQueueTask extends AbstractTask<PlayerQueue> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public enum QueueAction {
		CLEAR, SHUFFLE;
	}
	
	public enum QueueItemAction {
		TOP, UP, REMOVE, DOWN, BOTTOM;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected final PlayerReference playerReference;
	private final PlayerQueueChangeListener changeListener;
	
	QueueAction queueAction = null;
	QueueItemAction queueItemAction = null;
	Artifact item = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public GetPlayerQueueTask (Activity activity, PlayerReference playerReference, PlayerQueueChangeListener changeListener) {
		super(activity);
		this.playerReference = playerReference;
		this.changeListener = changeListener;
	}
	
	public GetPlayerQueueTask (Activity activity, PlayerReference playerReference, PlayerQueueChangeListener changeListener, QueueAction action) {
		this(activity, playerReference, changeListener);
		if (action == null) throw new IllegalArgumentException();
		this.queueAction = action;
	}
	
	public GetPlayerQueueTask (Activity activity, PlayerReference playerReference, PlayerQueueChangeListener changeListener, QueueItemAction action, Artifact item) {
		this(activity, playerReference, changeListener);
		if (action == null) throw new IllegalArgumentException();
		if (item == null) throw new IllegalArgumentException("item is null");
		this.queueItemAction = action;
		this.item = item;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected HttpCreds getCreds () {
		return this.playerReference.getServerReference();
	}
	
	@Override
	protected String getUrl () {
		String url = this.playerReference.getBaseUrl().concat(Constants.CONTEXT_PLAYER_QUEUE);
		if (this.item != null) {
			url = url.concat("/" + this.item.getId());
		}
		return url;
	}
	
	@Override
	protected String getVerb () {
		if (this.queueAction != null || this.queueItemAction != null) {
			return "POST";
		}
		return "GET";
	}
	
	@Override
	protected String getEncodedData () {
		if (this.queueAction != null) {
			switch (this.queueAction) {
				case CLEAR:   return "action=clear";
				case SHUFFLE: return "action=shuffle";
			}
		}
		else if (this.queueItemAction != null) {
			switch (this.queueItemAction) {
				case TOP:    return "action=top";
				case UP:     return "action=up";
				case REMOVE: return "action=remove";
				case DOWN:   return "action=down";
				case BOTTOM: return "action=bottom";
			}
		}
		return null;
	}
	
	@Override
	protected String getContentType () {
		return "application/x-www-form-urlencoded";
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
