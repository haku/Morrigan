package net.sparktank.morrigan.helpers;

public class TimeHelper {
	
	public static String formatTime (long seconds) {
		String s = "0" + seconds % 60;
		s = s.substring(s.length()-2);
		int m = (int) Math.floor(seconds/60f);
		return m + ":" + s;
	}
	
}
