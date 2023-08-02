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
import java.util.Collections;
import java.util.Map;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.UriHelper;
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.PlayState;
import com.vaguehope.morrigan.android.model.PlayerReference;
import com.vaguehope.morrigan.android.model.PlayerState;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.playback.MediaTag;

public class PlayerStateBasicImpl implements PlayerState, PlayerReference, MlistItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private String playerId;
	private String playerName;
	private String baseUrl;
	private PlayerReference playerReference;

	private PlayState playState;
	private int playOrder;
	private int playerPosition;

	private String trackRelativeUrl;
	private String trackTitle;
	private String trackFile;
	private String trackFileName;
	private int trackDuration;
	private long trackFileSize;
	private BigInteger trackHashCode;
	private boolean trackEnabled;
	private boolean trackMissing;
	private int trackStartCount;
	private int trackEndCount;

	private String listTitle;
	private String listId;
	private String listUrl;

	private int queueLength;
	private long queueDuration;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getTitle () {
		String ret = getName();
		String title = getTrackTitle();
		if (title != null && title.length() > 0) {
			ret = ret + ": " + title;
		}
		return ret;
	}

	@Override
	public int getImageResource () {
		return getImageResource(getPlayState());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void setPlayerReference (final PlayerReference playerReference) {
		this.playerReference = playerReference;
	}

	@Override
	public PlayerReference getPlayerReference () {
		return this.playerReference;
	}

	@Override
	public String getPlayerId () {
		return this.playerId;
	}

	@Override
	public String getName () {
		return this.playerName;
	}

	public void setName (final String name) {
		this.playerName = name;
	}

	@Override
	public ServerReference getServerReference () {
		return this.playerReference.getServerReference();
	}

	public void setBaseUrl (final String baseUrl) {
		this.baseUrl = UriHelper.ensureNoTrailingSlash(baseUrl);
	}

	@Override
	public String getBaseUrl () {
		return this.baseUrl;
	}

	@Override
	public String getId () {
		return this.playerId;
	}

	public void setId (final String id) {
		this.playerId = id;
	}

	@Override
	public PlayState getPlayState () {
		return this.playState;
	}

	public void setPlayState (final PlayState playState) {
		this.playState = playState;
	}

	@Override
	public int getPlayOrder () {
		return this.playOrder;
	}

	public void setPlayOrder (final int playOrder) {
		this.playOrder = playOrder;
	}

	@Override
	public int getPlayerPosition () {
		return this.playerPosition;
	}

	public void setPlayerPosition (final int playerPosition) {
		this.playerPosition = playerPosition;
	}

	@Override
	public String getTrackRelativeUrl () {
		return this.trackRelativeUrl;
	}

	public void setTrackRelativeUrl (final String trackRelativeUrl) {
		this.trackRelativeUrl = trackRelativeUrl;
	}

	@Override
	public String getTrackTitle () {
		return this.trackTitle;
	}

	public void setTrackTitle (final String trackTitle) {
		this.trackTitle = trackTitle;
	}

	@Override
	public String getTrackFile () {
		return this.trackFile;
	}

	public void setTrackFile (final String trackFile) {
		this.trackFile = trackFile;
	}

	@Override
	public String getTrackFileName () {
		return this.trackFileName;
	}

	public void setTrackFileName (final String trackFileName) {
		this.trackFileName = trackFileName;
	}

	@Override
	public int getTrackDuration () {
		return this.trackDuration;
	}

	public void setTrackDuration (final int trackDuration) {
		this.trackDuration = trackDuration;
	}

	public long getTrackFileSize () {
		return this.trackFileSize;
	}

	public void setTrackFileSize (final long trackFileSize) {
		this.trackFileSize = trackFileSize;
	}

	@Override
	public BigInteger getTrackHashCode () {
		return this.trackHashCode;
	}

	public void setTrackHashCode (final BigInteger trackHashCode) {
		this.trackHashCode = trackHashCode;
	}

	@Override
	public boolean getTrackEnabled () {
		return this.trackEnabled;
	}

	public void setTrackEnabled (final boolean trackEnabled) {
		this.trackEnabled = trackEnabled;
	}

	@Override
	public boolean getTrackMissing () {
		return this.trackMissing;
	}

	public void setTrackMissing (final boolean trackMissing) {
		this.trackMissing = trackMissing;
	}

	@Override
	public int getTrackStartCount () {
		return this.trackStartCount;
	}

	public void setTrackStartCount (final int trackStartCount) {
		this.trackStartCount = trackStartCount;
	}

	@Override
	public int getTrackEndCount () {
		return this.trackEndCount;
	}

	public void setTrackEndCount (final int trackEndCount) {
		this.trackEndCount = trackEndCount;
	}

	@Override
	public Collection<MediaTag> getTrackTags () {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getListTitle () {
		return this.listTitle;
	}

	public void setListTitle (final String listTitle) {
		this.listTitle = listTitle;
	}

	@Override
	public String getListId () {
		return this.listId;
	}

	public void setListId (final String listId) {
		this.listId = listId;
	}

	@Override
	public String getListUrl () {
		return this.listUrl;
	}

	public void setListUrl (final String listUrl) {
		this.listUrl = listUrl;
	}

	@Override
	public MlistItem getItem () {
		return this;
	}

	@Override
	public int getQueueLength () {
		return this.queueLength;
	}

	public void setQueueLength (final int queueLength) {
		this.queueLength = queueLength;
	}

	@Override
	public long getQueueDuration () {
		return this.queueDuration;
	}

	public void setQueueDuration (final long queueDuration) {
		this.queueDuration = queueDuration;
	}

	@Override
	public Map<Integer, String> getMonitors () {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MlistItem methods.

	@Override
	public int getType () {
		return -1;
	}

	@Override
	public String getRelativeUrl () {
		return getTrackRelativeUrl();
	}

	@Override
	public String getFileName () {
		return getTrackFileName();
	}

	@Override
	public long getFileSize () {
		return getTrackFileSize();
	}

	@Override
	public long getTimeAdded () {
		throw new UnsupportedOperationException("Not implemented.");
	}
	@Override
	public long getLastModified () {
			throw new UnsupportedOperationException("Not implemented.");
	}
	@Override
	public long getLastPlayed () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public BigInteger getOriginalHashCode () {
		return getTrackHashCode(); // FIXME Currently player is always the original hash.
	}

	@Override
	public BigInteger getHashCode () {
		return getTrackHashCode();
	}

	@Override
	public boolean isEnabled () {
		return getTrackEnabled();
	}

	@Override
	public boolean isMissing () {
		return getTrackMissing();
	}

	@Override
	public int getDuration () {
		return getTrackDuration();
	}

	@Override
	public int getStartCount () {
		return getTrackStartCount();
	}

	@Override
	public int getEndCount () {
		return getTrackEndCount();
	}

	@Override
	public Collection<MediaTag> getTags () {
		return Collections.emptyList();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static int getImageResource (final PlayState playState) {
		switch (playState) {
			case STOPPED:
				return R.drawable.stop;
			case PLAYING:
				return R.drawable.play;
			case PAUSED:
				return R.drawable.pause;
			case LOADING:
				return R.drawable.db; // TODO find better icon.
			default:
				throw new IllegalArgumentException();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
