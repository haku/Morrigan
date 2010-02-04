package net.sparktank.morrigan.helpers;

public class EqualHelper {
	
	static public boolean areEqual(Object aThis, Object aThat){
		return aThis == null ? aThat == null : aThis.equals(aThat);
	}
	
}
