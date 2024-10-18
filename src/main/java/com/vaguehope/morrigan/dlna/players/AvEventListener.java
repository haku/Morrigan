package com.vaguehope.morrigan.dlna.players;

import org.jupnp.support.model.TransportState;

public interface AvEventListener {

	void onTransportState (TransportState transportState);

}
