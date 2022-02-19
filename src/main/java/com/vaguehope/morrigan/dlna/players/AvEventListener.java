package com.vaguehope.morrigan.dlna.players;

import org.fourthline.cling.support.model.TransportState;

public interface AvEventListener {

	void onTransportState (TransportState transportState);

}
