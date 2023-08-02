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
import java.util.Collection;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.TimeHelper;
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.playback.MediaTag;


public class MlistItemBasicImpl implements MlistItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private String title;
	private int type;
	private String id;
	private String relativeUrl;
	private long fileSize;
	private long timeAdded;
	private long lastModified;
	private long lastPlayed;
	private BigInteger originalHashCode;
	private BigInteger hashCode;
	private boolean enabled;
	private boolean missing;
	private int duration;
	private int startCount;
	private int endCount;
	private Collection<MediaTag> tags;

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

	public void setTrackTitle(final String title) {
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
	public void setType(final int type) {
		this.type = type;
	}

	@Override
	public String getId () {
		return this.id;
	}
	public void setId (final String id) {
		this.id = id;
	}

	@Override
	public String getRelativeUrl() {
		return this.relativeUrl;
	}
	public void setRelativeUrl(final String url) {
		this.relativeUrl = url;
	}

	@Override
	public long getFileSize () {
		return this.fileSize;
	}
	public void setFileSize (final long fileSize) {
		this.fileSize = fileSize;
	}

	@Override
	public long getTimeAdded () {
		return this.timeAdded;
	}
	public void setTimeAdded (final long timeAdded) {
		this.timeAdded = timeAdded;
	}

	@Override
	public long getLastModified () {
		return this.lastModified;
	}
	public void setLastModified (final long lastModified) {
		this.lastModified = lastModified;
	}

	@Override
	public long getLastPlayed () {
		return this.lastPlayed;
	}
	public void setLastPlayed (final long lastPlayed) {
		this.lastPlayed = lastPlayed;
	}

	@Override
	public BigInteger getHashCode () {
		return this.hashCode;
	}
	public void setHashCode (final BigInteger hashCode) {
		this.hashCode = hashCode;
	}

	@Override
	public BigInteger getOriginalHashCode () {
		return this.originalHashCode;
	}
	public void setOriginalHashCode (final BigInteger originalHashCode) {
		this.originalHashCode = originalHashCode;
	}

	@Override
	public boolean isEnabled () {
		return this.enabled;
	}
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isMissing () {
		return this.missing;
	}
	public void setMissing(final boolean missing) {
		this.missing = missing;
	}

	@Override
	public int getDurationSeconds() {
		return this.duration;
	}
	public void setDurationSeconds(final int duration) {
		this.duration = duration;
	}

	@Override
	public int getStartCount() {
		return this.startCount;
	}
	public void setStartCount(final int startCount) {
		this.startCount = startCount;
	}

	@Override
	public int getEndCount() {
		return this.endCount;
	}
	public void setEndCount(final int endCount) {
		this.endCount = endCount;
	}

	@Override
	public Collection<MediaTag> getTags () {
		return this.tags;
	}
	public void setTags (final Collection<MediaTag> tags) {
		this.tags = tags;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
