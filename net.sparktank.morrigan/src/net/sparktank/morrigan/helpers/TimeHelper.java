package net.sparktank.morrigan.helpers;

public class TimeHelper {
	
	public static String formatTime (long x) {
//		String s = "0" + x % 60;
//		s = s.substring(s.length()-2);
//		int m = (int) Math.floor(x/60f);
//		return m + ":" + s;
		
		if (x >= 3600) {
			return String.format("%d:%02d:%02d", x / 3600, (x % 3600) / 60, (x % 60));
		} else {
			return String.format("%d:%02d", (x % 3600) / 60, (x % 60));
		}
	}
	
}
