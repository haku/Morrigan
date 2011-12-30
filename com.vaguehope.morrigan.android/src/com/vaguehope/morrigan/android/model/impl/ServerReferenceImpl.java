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

import com.vaguehope.morrigan.android.Constants;
import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.model.ServerReference;

public class ServerReferenceImpl implements ServerReference {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private int dbId;
	private final String name;
	private final String baseUrl;
	private final String pass;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public ServerReferenceImpl (String name, String baseUrl, String pass) {
		this(Integer.MIN_VALUE, name, baseUrl, pass);
	}
	
	public ServerReferenceImpl (int dbId, String name, String baseUrl, String pass) {
		this.dbId = dbId;
		this.name = name;
		this.baseUrl = baseUrl;
		this.pass = pass;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ServerReference methods.
	
	@Override
	public String getName () {
		return this.name;
	}
	
	@Override
	public String getBaseUrl () {
		return this.baseUrl;
	}
	
	@Override
	public String getPass () {
		return this.pass;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Artifact methods.
	
	@Override
	public int getId () {
		if (this.dbId == Integer.MIN_VALUE) throw new IllegalStateException("ID not set.");
		return this.dbId;
	}
	
	@Override
	public String getTitle () {
		return getName();
	}
	
	@Override
	public int getImageResource () {
		return R.drawable.db;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	HttpCreds methods.
	
	@Override
	public String getUser () {
		return Constants.USERNAME;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
