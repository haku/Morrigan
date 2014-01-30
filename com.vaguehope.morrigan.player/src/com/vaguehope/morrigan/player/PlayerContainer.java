package com.vaguehope.morrigan.player;

public interface PlayerContainer {

	String getName ();

	LocalPlayerSupport getLocalPlayerSupport ();

	void setPlayer (Player player);

	Player getPlayer ();

}
