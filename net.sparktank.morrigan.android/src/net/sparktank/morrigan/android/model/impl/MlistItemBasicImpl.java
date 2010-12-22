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
import net.sparktank.morrigan.android.model.MlistItem;

public class MlistItemBasicImpl implements MlistItem {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String title;
	private int type;
	private String relativeUrl;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getTitle() {
		return this.title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	public int getImageResource() {
//		if (mi.isMissing()) {
//			cell.setImage(null); // TODO find icon for missing?
//		}
//		else if (!mi.isEnabled()) {
//			cell.setImage(this.imageCache.readImage("icons/noentry-red.png"));
//		}
//		else if (mi.getHashcode() == 0) {
//			cell.setImage(this.imageCache.readImage("icons/exclamation-red.png"));
//		}
//		else {
			return R.drawable.circledot;
//		}
	}
	
	@Override
	public int getType() {
		return this.type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public void setRelativeUrl(String url) {
		this.relativeUrl = url;
	}
	
	@Override
	public String getRelativeUrl() {
		return this.relativeUrl;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
