package net.sparktank.morrigan.model.helper;

public class EqualHelper {
	
	public static boolean areEqual(Object aThis, Object aThat){
		return aThis == null ? aThat == null : aThis.equals(aThat);
	}
	
}
