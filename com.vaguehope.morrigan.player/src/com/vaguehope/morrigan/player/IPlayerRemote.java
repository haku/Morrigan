package com.vaguehope.morrigan.player;


public interface IPlayerRemote extends IPlayerAbstract {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	boolean isAvailable ();

	String getRemoteHost ();
	int getRemotePlayerId ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
