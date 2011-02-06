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

package net.sparktank.morrigan.android.model;

public interface PlayerState extends Artifact {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public PlayerReference getPlayerReference ();
	
	/**
	 * Get the player id.
	 */
	public int getId ();
	
	public PlayState getPlayState ();
	public int getPlayOrder (); // TODO replace with enum.
	public int getPlayerPosition ();
	
	public String getListTitle ();
	public String getListId ();
	public String getListUrl ();
	
	public String getTrackTitle ();
	public String getTrackFile ();
	public int getTrackDuration ();
	
	public int getQueueLength ();
	// TODO queueduration
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -	
}
