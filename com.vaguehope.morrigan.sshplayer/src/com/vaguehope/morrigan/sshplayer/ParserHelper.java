package com.vaguehope.morrigan.sshplayer;

public final class ParserHelper {

	private ParserHelper () {}

	public static int findNextDigit (String s, int start) {
		for (int i = start; i < s.length(); i++) {
			if (Character.isDigit(s.charAt(i))) return i;
		}
		return s.length();
	}

	public static int findNextNonDigit (String s, int start) {
		for (int i = start; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) return i;
		}
		return s.length();
	}

	public static int findNextSpace (String s, int start) {
		for (int i = start; i < s.length(); i++) {
			if (Character.isSpaceChar(s.charAt(i))) return i;
		}
		return s.length();
	}

}
