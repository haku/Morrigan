package net.sparktank.morrigan.helpers;

/**
 * FIXME do I really need this interface???
 */
public interface RecycliableProduct<K extends Object> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean isMadeWithThisMaterial (K material);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
