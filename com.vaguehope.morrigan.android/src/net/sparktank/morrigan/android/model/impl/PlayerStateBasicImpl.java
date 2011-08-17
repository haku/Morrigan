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

package net.sparktank.morrigan.android.model.impl;

import java.math.BigInteger;
import java.util.Map;

import net.sparktank.morrigan.android.R;
import net.sparktank.morrigan.android.model.MlistItem;
import net.sparktank.morrigan.android.model.PlayState;
import net.sparktank.morrigan.android.model.PlayerReference;
import net.sparktank.morrigan.android.model.PlayerState;
import net.sparktank.morrigan.android.model.ServerReference;

public class PlayerStateBasicImpl implements PlayerState, PlayerReference, MlistItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private int playerId;
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
	private BigInteger trackHashCode;
	private int trackStartCount;
	private int trackEndCount;
	
	private String listTitle;
	private String listId;
	private String listUrl;
	
	private int queueLength;
	private long queueDuration;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		return getTrackTitle();
	}
	
	@Override
	public int getImageResource() {
		return getImageResource(getPlayState());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setPlayerReference(PlayerReference playerReference) {
		this.playerReference = playerReference;
	}
	@Override
	public PlayerReference getPlayerReference() {
		return this.playerReference;
	}
	
	@Override
	public int getPlayerId() {
		return this.playerId;
	}
	
	@Override
	public ServerReference getServerReference() {
		return this.playerReference.getServerReference();
	}
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	
	@Override
	public String getBaseUrl() {
		return this.baseUrl;
	}
	
	@Override
	public int getId() {
		return this.playerId;
	}
	public void setId(int id) {
		this.playerId = id;
	}
	
	@Override
	public PlayState getPlayState() {
		return this.playState;
	}
	public void setPlayState(PlayState playState) {
		this.playState = playState;
	}

	@Override
	public int getPlayOrder() {
		return this.playOrder;
	}
	public void setPlayOrder(int playOrder) {
		this.playOrder = playOrder;
	}

	@Override
	public int getPlayerPosition() {
		return this.playerPosition;
	}
	public void setPlayerPosition(int playerPosition) {
		this.playerPosition = playerPosition;
	}
	
	@Override
	public String getTrackRelativeUrl () {
		return this.trackRelativeUrl;
	}
	public void setTrackRelativeUrl (String trackRelativeUrl) {
		this.trackRelativeUrl = trackRelativeUrl;
	}
	
	@Override
	public String getTrackTitle() {
		return this.trackTitle;
	}
	public void setTrackTitle(String trackTitle) {
		this.trackTitle = trackTitle;
	}

	@Override
	public String getTrackFile() {
		return this.trackFile;
	}
	public void setTrackFile(String trackFile) {
		this.trackFile = trackFile;
	}
	
	@Override
	public String getTrackFileName () {
		return this.trackFileName;
	}
	public void setTrackFileName (String trackFileName) {
		this.trackFileName = trackFileName;
	}
	
	@Override
	public int getTrackDuration() {
		return this.trackDuration;
	}
	public void setTrackDuration(int trackDuration) {
		this.trackDuration = trackDuration;
	}
	
	@Override
	public BigInteger getTrackHashCode () {
		return this.trackHashCode;
	}
	public void setTrackHashCode (BigInteger trackHashCode) {
		this.trackHashCode = trackHashCode;
	}
	
	@Override
	public int getTrackStartCount () {
		return this.trackStartCount;
	}
	public void setTrackStartCount (int trackStartCount) {
		this.trackStartCount = trackStartCount;
	}
	
	@Override
	public int getTrackEndCount () {
		return this.trackEndCount;
	}
	public void setTrackEndCount (int trackEndCount) {
		this.trackEndCount = trackEndCount;
	}
	
	@Override
	public String getListTitle() {
		return this.listTitle;
	}
	public void setListTitle(String listTitle) {
		this.listTitle = listTitle;
	}

	@Override
	public String getListId() {
		return this.listId;
	}
	public void setListId(String listId) {
		this.listId = listId;
	}

	@Override
	public String getListUrl() {
		return this.listUrl;
	}
	public void setListUrl(String listUrl) {
		this.listUrl = listUrl;
	}
	
	@Override
	public MlistItem getItem () {
		return this;
	}
	
	@Override
	public int getQueueLength() {
		return this.queueLength;
	}
	public void setQueueLength(int queueLength) {
		this.queueLength = queueLength;
	}
	
	@Override
	public long getQueueDuration() {
		return this.queueDuration;
	}
	public void setQueueDuration (long queueDuration) {
		this.queueDuration = queueDuration;
	}
	
	@Override
	public Map<Integer, String> getMonitors() {
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
	public BigInteger getHashCode () {
		return getTrackHashCode();
	}
	
	@Override
	public int getDuration () {
		return getTrackDuration();
	}
	
	@Override
	public int getStartCount () {
		return getStartCount();
	}
	
	@Override
	public int getEndCount () {
		return getEndCount();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public int getImageResource (PlayState playState) {
		switch (playState) {
			case STOPPED: return R.drawable.stop;
			case PLAYING: return R.drawable.play;
			case PAUSED:  return R.drawable.pause;
			case LOADING: return R.drawable.db; // TODO find better icon.
			default: throw new IllegalArgumentException();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
