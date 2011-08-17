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

package com.vaguehope.morrigan.android.model.impl;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.model.ServerReference;

public class ServerReferenceImpl implements ServerReference {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String baseUrl;
	
	private long dbId;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public ServerReferenceImpl (String baseUrl) {
		if (baseUrl == null) throw new IllegalArgumentException();
		
		this.baseUrl = baseUrl;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ServerReference methods.
	
	@Override
	public String getBaseUrl() {
		return this.baseUrl;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Artifact methods.
	
	@Override
	public String getTitle() {
		return getBaseUrl();
	}
	
	@Override
	public int getImageResource() {
		return R.drawable.db;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setDbId(long dbId) {
		this.dbId = dbId;
	}
	
	public long getDbId() {
		return this.dbId;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
