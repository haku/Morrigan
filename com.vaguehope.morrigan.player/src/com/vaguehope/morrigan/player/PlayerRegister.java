package com.vaguehope.morrigan.player;

import java.util.Collection;

import com.vaguehope.morrigan.model.Register;


public interface PlayerRegister extends Register<Player> {

	Collection<Player> getAll ();
	Player get (String id);
	LocalPlayer makeLocal (String name, LocalPlayerSupport localPlayerSupport);
	LocalPlayer makeLocalProxy(Player player, LocalPlayerSupport localPlayerSupport);

}
