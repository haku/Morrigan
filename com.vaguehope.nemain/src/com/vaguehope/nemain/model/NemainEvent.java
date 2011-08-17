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

package com.vaguehope.nemain.model;

import com.vaguehope.nemain.helpers.EqualHelper;


public class NemainEvent extends NemainDate {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public NemainEvent (String entryText, int entryYear, int entryMonth, int entryDay) {
		super(entryYear, entryMonth, entryDay);
		this.entryText = entryText;
	}
	
	public NemainEvent (String entryText, NemainDate date) {
		super(date);
		this.entryText = entryText;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Properties.
	
	private String entryText;
	
	public String getEntryText() {
		return this.entryText;
	}
	
	@Override
	public boolean isMutable() {
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(super.toString());
		sb.append(' ');
		sb.append(getEntryText());
		
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object aThat) {
		if ( aThat == null ) return false;
		if ( this == aThat ) return true;
		if ( !(aThat instanceof NemainEvent) ) return false;
		NemainEvent that = (NemainEvent)aThat;
		
		return super.equals(aThat)
			&& EqualHelper.areEqual(this.entryText, that.getEntryText());
	}
	
	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + super.hashCode();
		hash = hash * 31 + (this.entryText == null ? 0 : this.entryText.hashCode());
		return hash;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
