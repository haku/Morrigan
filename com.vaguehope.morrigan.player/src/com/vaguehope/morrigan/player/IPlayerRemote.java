package com.vaguehope.morrigan.player;

/**
 * TODO rename to RemotePlayer
 */
public interface IPlayerRemote extends Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	boolean isAvailable ();

	String getRemoteHost ();
	int getRemotePlayerId ();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
