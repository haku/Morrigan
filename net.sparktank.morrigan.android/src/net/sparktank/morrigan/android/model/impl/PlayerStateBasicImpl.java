package net.sparktank.morrigan.android.model.impl;

import net.sparktank.morrigan.android.model.PlayState;
import net.sparktank.morrigan.android.model.PlayerState;

public class PlayerStateBasicImpl implements PlayerState {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private int id;
	
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
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
