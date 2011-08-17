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

package com.vaguehope.morrigan.android.helper;

public class TimeHelper {
	
	@SuppressWarnings("boxing")
	static public String formatTimeSeconds (long seconds) {
//		String s = "0" + x % 60;
//		s = s.substring(s.length()-2);
//		int m = (int) Math.floor(x/60f);
//		return m + ":" + s;
		
		if (seconds >= 3600) {
			return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
		}
		
		return String.format("%d:%02d", (seconds % 3600) / 60, (seconds % 60));
	}
	
	public static String formatTimeMiliseconds (long miliseconds) {
		return String.valueOf(miliseconds/1000f);
	}
	
}
