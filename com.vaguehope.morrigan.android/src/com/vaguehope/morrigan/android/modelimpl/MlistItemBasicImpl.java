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

import java.math.BigInteger;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.TimeHelper;
import com.vaguehope.morrigan.android.model.MlistItem;


public class MlistItemBasicImpl implements MlistItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String title;
	private int type;
	private int id;
	private String relativeUrl;
	private BigInteger hashCode;
	private boolean enabled;
	private boolean missing;
	private int duration;
	private int startCount;
	private int endCount;
	private String[] tags;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		if (this.duration > 0) {
			return this.title + " (" + TimeHelper.formatTimeSeconds(this.duration) + ")";
		}
		return this.title;
	}
	
	public String getTrackTitle() {
		return this.title;
	}
	
	public void setTrackTitle(String title) {
		this.title = title;
	}
	
	@Override
	public String getFileName () {
		return this.title; // TODO name this more explicitly?
	}
	
	@Override
	public int getImageResource() {
		if (isMissing()) {
			return 0; // TODO find icon for missing?
		}
		else if (!isEnabled()) {
			return R.drawable.noentry_red;
		}
		else if (getHashCode() == null || getHashCode().equals(BigInteger.ZERO)) {
			return R.drawable.exclamation_red;
		}
		return R.drawable.circledot;
	}
	
	@Override
	public int getType() {
		return this.type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	@Override
	public int getId () {
		return this.id;
	}
	public void setId (int id) {
		this.id = id;
	}
	
	@Override
	public String getRelativeUrl() {
		return this.relativeUrl;
	}
	public void setRelativeUrl(String url) {
		this.relativeUrl = url;
	}
	
	@Override
	public BigInteger getHashCode () {
		return this.hashCode;
	}
	public void setHashCode (BigInteger hashCode) {
		this.hashCode = hashCode;
	}
	
	@Override
	public boolean isEnabled () {
		return this.enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public boolean isMissing () {
		return this.missing;
	}
	public void setMissing(boolean missing) {
		this.missing = missing;
	}
	
	@Override
	public int getDuration() {
		return this.duration;
	}
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	@Override
	public int getStartCount() {
		return this.startCount;
	}
	public void setStartCount(int startCount) {
		this.startCount = startCount;
	}
	
	@Override
	public int getEndCount() {
		return this.endCount;
	}
	public void setEndCount(int endCount) {
		this.endCount = endCount;
	}
	
	@Override
	public String[] getTags () {
		return this.tags;
	}
	public void setTags (String[] tags) {
		this.tags = tags;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
