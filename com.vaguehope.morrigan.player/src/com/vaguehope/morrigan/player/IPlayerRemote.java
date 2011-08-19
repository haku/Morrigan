package com.vaguehope.morrigan.player;


public interface IPlayerRemote extends IPlayerAbstract {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public boolean isAvailable ();
	
	public String getRemoteHost ();
	public int getRemotePlayerId ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
