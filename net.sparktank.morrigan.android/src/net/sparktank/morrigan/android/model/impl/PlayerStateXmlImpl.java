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

import org.xml.sax.SAXException;

import net.sparktank.morrigan.android.R;
import net.sparktank.morrigan.android.helper.XmlParser;
import net.sparktank.morrigan.android.model.PlayState;
import net.sparktank.morrigan.android.model.PlayerState;

public class PlayerStateXmlImpl extends XmlParser implements PlayerState {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TRACKDURATION = "trackduration";
	public static final String TRACKFILE = "trackfile";
	public static final String PLAYPOSITION = "playposition";
	public static final String TRACKTITLE = "tracktitle";
	public static final String LISTID = "listid";
	public static final String LISTTITLE = "listtitle";
	public static final String QUEUEDURATION = "queueduration";
	public static final String QUEUELENGTH = "queuelength";
	public static final String PLAYORDER = "playorder";
	public static final String PLAYSTATE = "playstate";
	public static final String PLAYERID = "playerid";
	
	public final static String[] nodes = new String[] { 
		PLAYERID,
		PLAYSTATE,
		PLAYORDER,
		QUEUELENGTH,
        QUEUEDURATION,
        LISTTITLE,
        LISTID,
        TRACKTITLE,
        PLAYPOSITION,
        TRACKFILE,
        TRACKDURATION
		};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PlayerStateXmlImpl (String xmlString) throws SAXException {
		super(xmlString, nodes);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		return getTrackTitle();
	}
	
	@Override
	public int getImageResource() {
		return R.drawable.play;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public int getId() {
		return this.getNodeInt(PLAYERID);
	}
	
	@Override
	public PlayState getPlayState() {
		return PlayState.parseN(this.getNodeInt(PLAYSTATE));
	}
	
	@Override
	public int getPlayOrder() {
		return this.getNodeInt(PLAYORDER);
	}
	
	@Override
	public int getPlayerPosition() {
		return this.getNodeInt(PLAYPOSITION);
	}
	
	@Override
	public String getListTitle() {
		return this.getNode(LISTTITLE);
	}
	
	@Override
	public String getListId() {
		return this.getNode(LISTID);
	}
	
	@Override
	public String getTrackTitle() {
		return this.getNode(TRACKTITLE);
	}
	
	@Override
	public String getTrackFile() {
		return this.getNode(TRACKFILE);
	}
	
	@Override
	public int getTrackDuration() {
		return this.getNodeInt(TRACKDURATION);
	}
	
	@Override
	public int getQueueLength() {
		return this.getNodeInt(QUEUELENGTH);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
