package com.vaguehope.morrigan.player;

public interface PlayerContainer {

	String getName ();

	PlayerEventHandler getEventHandler ();

	void setPlayer (Player player);

	Player getPlayer ();

}
