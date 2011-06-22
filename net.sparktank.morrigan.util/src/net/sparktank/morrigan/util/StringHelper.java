package net.sparktank.morrigan.util;

import java.util.Collection;

public class StringHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private StringHelper () { /* UNUSED */ }
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static <T> String joinCollection (Collection<T> collection, String delim) {
		StringBuilder sb = new StringBuilder();
		
		for (T i : collection) {
			sb.append(i.toString());
			sb.append(delim);
		}
		sb.delete(sb.length() - delim.length(), sb.length());
		
		return sb.toString();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
