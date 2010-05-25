package net.sparktank.morrigan.helpers;

public class TimeHelper {
	
	public static String formatTimeSeconds (long seconds) {
//		String s = "0" + x % 60;
//		s = s.substring(s.length()-2);
//		int m = (int) Math.floor(x/60f);
//		return m + ":" + s;
		
		if (seconds >= 3600) {
			return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60));
		} else {
			return String.format("%d:%02d", (seconds % 3600) / 60, (seconds % 60));
		}
	}
	
	public static String formatTimeMiliseconds (long miliseconds) {
		return String.valueOf(miliseconds/1000f);
	}
	
}
