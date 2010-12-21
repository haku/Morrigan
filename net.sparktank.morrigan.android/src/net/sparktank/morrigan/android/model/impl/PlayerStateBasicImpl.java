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

import net.sparktank.morrigan.android.R;
import net.sparktank.morrigan.android.model.PlayState;
import net.sparktank.morrigan.android.model.PlayerReference;
import net.sparktank.morrigan.android.model.PlayerState;

public class PlayerStateBasicImpl implements PlayerState, PlayerReference {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private int id;
	
	private String baseUrl;
	
	private PlayState playState;
	private int playOrder;
	private int playerPosition;
	
	private String trackTitle;
	private String trackFile;
	private int trackDuration;
	
	private String listTitle;
	private String listId;
	
	private int queueLength;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		return "p" + getId() + ":" + getTrackTitle();
	}
	
	@Override
	public int getImageResource() {
		return R.drawable.play;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public String getBaseUrl() {
		return this.baseUrl;
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
	public int getTrackDuration() {
		return this.trackDuration;
	}

	public void setTrackDuration(int trackDuration) {
		this.trackDuration = trackDuration;
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
	public int getQueueLength() {
		return this.queueLength;
	}

	public void setQueueLength(int queueLength) {
		this.queueLength = queueLength;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
