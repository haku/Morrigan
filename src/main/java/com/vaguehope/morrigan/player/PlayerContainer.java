package com.vaguehope.morrigan.player;

public interface PlayerContainer {

	String getPrefix ();
	String getName ();

	void setPlayer (Player player);

	Player getPlayer ();

}
