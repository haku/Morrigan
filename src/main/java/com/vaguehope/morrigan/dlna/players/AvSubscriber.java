package com.vaguehope.morrigan.dlna.players;

import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.TransportState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.util.ErrorHelper;

public class AvSubscriber extends SubscriptionCallback {

	private static final Logger LOG = LoggerFactory.getLogger(AvSubscriber.class);
	private final AbstractDlnaPlayer dlnaPlayer;
	private final AvEventListener listener;

	protected AvSubscriber (final AbstractDlnaPlayer dlnaPlayer, final AvEventListener listener, final Service service, final int requestedDurationSeconds) {
		super(service, requestedDurationSeconds);
		this.dlnaPlayer = dlnaPlayer;
		this.listener = listener;
	}

	@Override
	public void established (final GENASubscription sub) {
		LOG.info("{} Established subscription: {}", this.dlnaPlayer.getId(), sub.getSubscriptionId());
	}

	@Override
	public void failed (final GENASubscription sub, final UpnpResponse response, final Exception ex, final String defaultMsg) {
		LOG.info("{} Subscription failed: {}", this.dlnaPlayer.getId(), defaultMsg);
		reconnectIfAlive();
	}

	@Override
	public void ended (final GENASubscription sub, final CancelReason reason, final UpnpResponse response) {
		if (reason == null) {
			LOG.info("{} Subscription ended.", this.dlnaPlayer.getId());
		}
		else {
			LOG.warn("{} Subscription ended: {} {}", this.dlnaPlayer.getId(), reason, response);
		}
		reconnectIfAlive();
	}

	private void reconnectIfAlive () {
		if (!this.dlnaPlayer.isDisposed()) {
			LOG.error("{} TODO: Restablish subscription.", this.dlnaPlayer.getId());
		}
	}

	@Override
	public void eventsMissed (final GENASubscription sub, final int numberOfMissedEvents) {
		LOG.warn("{} Missed {} subscription events.", this.dlnaPlayer.getId(), numberOfMissedEvents);
	}

	@Override
	public void eventReceived (final GENASubscription sub) {
		// Use this log event to see what types of event there are.
		LOG.debug("{} Event {}: {}", this.dlnaPlayer.getId(), sub.getCurrentSequence().getValue(), sub.getCurrentValues());
		try {
			final Object rawLastChange = sub.getCurrentValues().get("LastChange");
			if (rawLastChange != null) {
				final String fixedLastChange = fixLastChange(rawLastChange.toString());
				final LastChange lastChange;
				try {
					lastChange = new LastChange(
							new AVTransportLastChangeParser(),
							fixedLastChange);
				}
				catch (final Exception e) {
					LOG.warn("{} Failed to parse '{}': {}",
							this.dlnaPlayer.getId(),
							fixedLastChange,
							ErrorHelper.oneLineCauseTrace(e));
					return;
				}
				lastChangeReceived(lastChange);
			}
		}
		catch (final Exception e) {
			LOG.warn("{} Failed to subscription handle event: {}", this.dlnaPlayer.getId(), ErrorHelper.oneLineCauseTrace(e));
		}
	}

	private String fixLastChange (final String raw) {
		return raw.replace(
				"xmlns=\"urn:schemas-upnp-org:metadata-1-0/AVT_RCS\"",
				"xmlns=\"urn:schemas-upnp-org:metadata-1-0/AVT/\""); // AVTransportLastChangeParser.NAMESPACE_URI
	}

	private void lastChangeReceived (final LastChange lastChange) {
		// if there were multiple instances lastChange.getInstanceIDs() would be useful, but assumeing always instance 0.
		final AVTransportVariable.TransportState varTransportState = lastChange.getEventedValue(0, AVTransportVariable.TransportState.class);
		if (varTransportState != null) {
			final TransportState transportState = varTransportState.getValue();
			if (transportState != null) {
				LOG.info("{} transportState: {}", this.dlnaPlayer.getId(), transportState);
				this.listener.onTransportState(transportState);
			}
		}
	}

}
