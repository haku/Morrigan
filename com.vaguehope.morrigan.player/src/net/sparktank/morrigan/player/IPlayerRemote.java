package net.sparktank.morrigan.player;


public interface IPlayerRemote extends IPlayerAbstract {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean isAvailable ();
	
	public String getRemoteHost ();
	public int getRemotePlayerId ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
