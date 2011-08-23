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

package com.vaguehope.morrigan.android.model;

import java.math.BigInteger;

public interface MlistItem extends Artifact {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle ();
	@Override
	public int getImageResource ();
	
	public int getType ();
	@Override
	public int getId ();
	public String getRelativeUrl ();
	public String getFileName ();
	public BigInteger getHashCode ();
	public boolean isEnabled ();
	public boolean isMissing ();
	
	public int getDuration ();
	public int getStartCount ();
	public int getEndCount ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
