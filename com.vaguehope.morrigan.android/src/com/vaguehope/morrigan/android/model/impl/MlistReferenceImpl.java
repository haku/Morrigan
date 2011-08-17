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

package com.vaguehope.morrigan.android.model.impl;

import com.vaguehope.morrigan.android.model.MlistReference;
import com.vaguehope.morrigan.android.model.ServerReference;

public class MlistReferenceImpl implements MlistReference {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final String baseUrl;
	private final ServerReference serverReference;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MlistReferenceImpl (String baseUrl, ServerReference serverReference) {
		if (baseUrl == null) throw new IllegalArgumentException();
		if (serverReference == null) throw new IllegalArgumentException();
		
		this.baseUrl = baseUrl;
		this.serverReference = serverReference;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getBaseUrl() {
		return this.baseUrl;
	}
	
	@Override
	public ServerReference getServerReference() {
		return this.serverReference;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
