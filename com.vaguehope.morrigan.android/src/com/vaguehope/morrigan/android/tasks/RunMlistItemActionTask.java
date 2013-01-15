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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.widget.Toast;

import com.vaguehope.morrigan.android.C;
import com.vaguehope.morrigan.android.helper.HttpHelper.HttpCreds;
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.MlistReference;
import com.vaguehope.morrigan.android.model.PlayerReference;

public class RunMlistItemActionTask extends AbstractTask<String> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static enum MlistItemCommand {
		PLAY(0), QUEUE(1);

		private int n;

		private MlistItemCommand (int n) {
			this.n = n;
		}

		public int getN() {
			return this.n;
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final PlayerReference playerReference;
	private final MlistReference mlistReference;
	private final MlistItem mlistItem;
	private final MlistItemCommand cmd;
	private final String newTag;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public RunMlistItemActionTask (Activity activity, PlayerReference playerReference, MlistReference mlistReference, MlistItem mlistItem, MlistItemCommand cmd) {
		this(activity, playerReference, mlistReference, mlistItem, cmd, null);
	}

	public RunMlistItemActionTask (Activity activity, MlistReference mlistReference, MlistItem mlistItem, String newTag) {
		this(activity, null, mlistReference, mlistItem, null, newTag);
	}

	private RunMlistItemActionTask (Activity activity, PlayerReference playerReference, MlistReference mlistReference, MlistItem mlistItem, MlistItemCommand cmd, String newTag) {
		super(activity);
		this.playerReference = playerReference;
		this.mlistReference = mlistReference;
		this.mlistItem = mlistItem;
		this.cmd = cmd;
		this.newTag = newTag;
	}
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected String getProgressMsg () {
		if (this.cmd != null) {
			switch (this.cmd) {
				case PLAY: return "Playing...";
				case QUEUE: return "Queueing...";
				default: return "Please wait...";
			}
		}
		else if (this.newTag != null) {
			return "Tagging...";
		}
		else {
			return "Please wait...";
		}
	}

	@Override
	protected HttpCreds getCreds () {
		return this.mlistReference.getServerReference();
	}

	@Override
	protected String getUrl () {
		return this.mlistReference.getBaseUrl() + C.CONTEXT_MLIST_ITEMS + "/" + this.mlistItem.getRelativeUrl();
	}

	@Override
	protected String getVerb () {
		return "POST";
	}

	@Override
	protected String getEncodedData () {
		if (this.cmd != null) {
			String encodedData = "action=";
			switch (this.cmd) {
				case PLAY:
					encodedData = encodedData.concat("play");
					break;

				case QUEUE:
					encodedData = encodedData.concat("queue");
					break;

				default: throw new IllegalArgumentException();
			}
			encodedData = encodedData.concat("&playerid=" + String.valueOf(this.playerReference.getPlayerId()));
			return encodedData;
		}
		else if (this.newTag != null) {
			String encodedTag;
			try {
				encodedTag = URLEncoder.encode(this.newTag, "UTF-8");
			} catch (UnsupportedEncodingException e) { throw new RuntimeException(e); }
			return "action=addtag&tag=" + encodedTag;
		}
		else {
			throw new IllegalStateException();
		}
	}

	@Override
	protected String getContentType () {
		return "application/x-www-form-urlencoded";
	}

	@Override
	protected String parseStream (InputStream is) throws IOException, SAXException {
		return parseStreamToString(is);
	}

	// In UI thread:
	@Override
	protected void onSuccess (String result, Exception exception) {
		if (isShowProgress() && result != null) {
			Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();
		}
		if (isShowProgress() && exception != null) {
			Toast.makeText(getActivity(), exception.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
