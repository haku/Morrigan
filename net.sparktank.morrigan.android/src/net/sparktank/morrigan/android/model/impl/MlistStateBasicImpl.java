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

package net.sparktank.morrigan.android.model.impl;

import net.sparktank.morrigan.android.R;
import net.sparktank.morrigan.android.model.MlistReference;
import net.sparktank.morrigan.android.model.MlistState;

public class MlistStateBasicImpl implements MlistState, MlistReference {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String title;
	private String baseUrl;
	private int count;
	private long duration;
	private boolean durationComplete;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		return this.title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	public int getImageResource() {
		return R.drawable.db;
	}
	
	@Override
	public String getBaseUrl() {
		return this.baseUrl;
	}
	
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	
	@Override
	public int getCount() {
		return this.count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
	@Override
	public long getDuration() {
		return this.duration;
	}
	
	public void setDuration(long duration) {
		this.duration = duration;
	}
	
	public void setDurationComplete(boolean durationComplete) {
		this.durationComplete = durationComplete;
	}
	
	@Override
	public boolean isDurationComplete() {
		return this.durationComplete;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
