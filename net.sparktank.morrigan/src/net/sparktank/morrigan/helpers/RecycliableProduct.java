package net.sparktank.morrigan.helpers;

public interface RecycliableProduct<K extends Object> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean isMadeWithThisMaterial (K material);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
