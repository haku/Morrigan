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

package com.vaguehope.morrigan.android.modelimpl;

import com.vaguehope.morrigan.android.Constants;
import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.model.PlayerReference;
import com.vaguehope.morrigan.android.model.ServerReference;

public class PlayerReferenceImpl implements PlayerReference {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final int playerId;
	private final String baseUrl;
	private final ServerReference serverReference;
	private final String title;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PlayerReferenceImpl (ServerReference serverReference, int playerId) {
		if (serverReference == null) throw new IllegalArgumentException();
		
		this.playerId = playerId;
		this.baseUrl = serverReference.getBaseUrl() + Constants.CONTEXT_PLAYERS + "/" + playerId;
		this.serverReference = serverReference;
		
		this.title = this.serverReference.getName() + " / player " + this.playerId;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	PlayerReference
	
	@Override
	public int getPlayerId() {
		return this.playerId;
	}
	
	@Override
	public String getBaseUrl() {
		return this.baseUrl;
	}
	
	@Override
	public ServerReference getServerReference() {
		return this.serverReference;
	}

	@Override
	public int getId () {
		return getPlayerId();
	}

	@Override
	public String getTitle () {
		return this.title;
	}

	@Override
	public int getImageResource () {
		return R.drawable.play;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
