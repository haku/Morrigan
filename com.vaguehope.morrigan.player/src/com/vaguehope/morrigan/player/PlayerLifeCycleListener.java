package com.vaguehope.morrigan.player;

public interface PlayerLifeCycleListener {

	void playerCreated(Player player);
	void playerDisposed(Player player);

}
