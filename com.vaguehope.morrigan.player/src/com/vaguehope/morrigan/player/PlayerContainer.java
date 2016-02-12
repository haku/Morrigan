package com.vaguehope.morrigan.player;

public interface PlayerContainer {

	String getPrefix ();
	String getName ();

	LocalPlayerSupport getLocalPlayerSupport ();

	void setPlayer (Player player);

	Player getPlayer ();

}
