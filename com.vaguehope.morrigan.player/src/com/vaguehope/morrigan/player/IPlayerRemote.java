package com.vaguehope.morrigan.player;

/**
 * TODO rename to RemotePlayer
 */
public interface IPlayerRemote extends IPlayerAbstract {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	boolean isAvailable ();

	String getRemoteHost ();
	int getRemotePlayerId ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
