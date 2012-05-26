package com.vaguehope.morrigan.player;

public interface PlayerContainer {

	String getName ();

	IPlayerEventHandler getEventHandler ();

	void setPlayer (Player player);

	Player getPlayer ();

}
