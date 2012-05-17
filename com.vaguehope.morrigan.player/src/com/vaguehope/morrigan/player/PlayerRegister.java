package com.vaguehope.morrigan.player;

import java.util.Collection;

import com.vaguehope.morrigan.model.Register;


public interface PlayerRegister extends Register<IPlayerAbstract> {

	public Collection<IPlayerAbstract> getAll ();
	public IPlayerAbstract get (int i);
	public IPlayerLocal makeLocal (String name, IPlayerEventHandler eventHandler);

}
