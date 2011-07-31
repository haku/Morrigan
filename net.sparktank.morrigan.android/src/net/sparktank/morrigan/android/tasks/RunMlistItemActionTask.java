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

package net.sparktank.morrigan.android.tasks;

import java.io.IOException;
import java.io.InputStream;

import net.sparktank.morrigan.android.model.MlistItem;
import net.sparktank.morrigan.android.model.PlayerReference;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.widget.Toast;

public class RunMlistItemActionTask extends AbstractTask<String> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public enum MlistItemCommand {
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
	private final MlistItem mlistItem;
	private final MlistItemCommand cmd;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public RunMlistItemActionTask (Activity activity, PlayerReference playerReference, MlistItem mlistItem, MlistItemCommand cmd) {
		super(activity);
		this.playerReference = playerReference;
		this.mlistItem = mlistItem;
		this.cmd = cmd;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
		protected String getProgressMsg () {
		return "Please wait...";
		}
	
	@Override
	protected String getUrl () {
		return this.playerReference.getServerReference().getBaseUrl() + this.mlistItem.getRelativeUrl();
	}
	
	@Override
	protected String getVerb () {
		return "POST";
	}
	
	@Override
	protected String getEncodedData () {
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
	protected void onSuccess (String result) {
		Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
