package com.vaguehope.morrigan.player;

import java.util.Collection;

import com.vaguehope.morrigan.model.Register;


public interface PlayerRegister extends Register<IPlayerAbstract> {

	Collection<IPlayerAbstract> getAll ();
	IPlayerAbstract get (int i);
	IPlayerLocal makeLocal (String name, IPlayerEventHandler eventHandler);

}
