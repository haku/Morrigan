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

import android.app.Activity;
import android.widget.Toast;

import com.vaguehope.morrigan.android.helper.HttpHelper.HttpCreds;
import com.vaguehope.morrigan.android.model.MlistReference;
import com.vaguehope.morrigan.android.model.PlayerReference;

public class RunMlistActionTask extends AbstractTask<String> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	static public enum MlistCommand {
		PLAY(0), QUEUE(1), SCAN(2);

		private int n;

		private MlistCommand (int n) {
			this.n = n;
		}

		public int getN() {
			return this.n;
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final MlistReference mlistReference;
	private final MlistCommand cmd;
	private final PlayerReference playerReference;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public RunMlistActionTask (Activity activity, MlistReference mlistReference, MlistCommand cmd) {
		this(activity, mlistReference, cmd, null);
	}

	public RunMlistActionTask (Activity activity, MlistReference mlistReference, MlistCommand cmd, PlayerReference playerReference) {
		super(activity);

		if ((cmd == MlistCommand.PLAY || cmd == MlistCommand.QUEUE) && playerReference == null) throw new IllegalArgumentException("No player specified.");

		this.mlistReference = mlistReference;
		this.cmd = cmd;
		this.playerReference = playerReference;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	protected String getProgressMsg () {
		switch (this.cmd) {
			case PLAY: return "Playing...";
			case QUEUE: return "Queueing...";
			case SCAN: return "Starting scan...";
			default: return "Please wait...";
		}
	}

	@Override
	protected HttpCreds getCreds () {
		return this.mlistReference.getServerReference();
	}

	@Override
	protected String getUrl () {
		return this.mlistReference.getBaseUrl();
	}

	@Override
	protected String getEncodedData () {
		String encodedData = "action=";
		switch (this.cmd) {
			case PLAY:
				encodedData = encodedData.concat("play").concat("&playerid=" + this.playerReference.getPlayerId());
				break;

			case QUEUE:
				encodedData = encodedData.concat("queue").concat("&playerid=" + this.playerReference.getPlayerId());
				break;

			case SCAN:
				encodedData = encodedData.concat("scan");
				break;

			default: throw new IllegalStateException();
		}
		return encodedData;
	}

	@Override
	protected String getVerb () {
		return "POST";
	}

	@Override
	protected String getContentType () {
		return "application/x-www-form-urlencoded";
	}

	// In background thread:
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
