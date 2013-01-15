package com.vaguehope.morrigan.playbackimpl.gs;

public class FormatHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private FormatHelper () { /* Unused */ }
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static boolean mightFileHaveVideo (String f) {
		String ext = f.substring(f.lastIndexOf('.') + 1).toLowerCase();
		for (String e : Constants.AUDIO_ONLY_FORMATS) {
			if (e.equals(ext)) {
				return false;
			}
		}
		return true;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
