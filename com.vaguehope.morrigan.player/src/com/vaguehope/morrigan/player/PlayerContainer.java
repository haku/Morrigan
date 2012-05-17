package com.vaguehope.morrigan.player;

public interface PlayerContainer {

	String getName ();

	IPlayerEventHandler getEventHandler ();

	void setPlayer (IPlayerAbstract player);

	IPlayerAbstract getPlayer ();

}
