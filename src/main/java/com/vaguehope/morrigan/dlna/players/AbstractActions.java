package com.vaguehope.morrigan.dlna.players;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.dlna.DlnaTimeoutException;

public class AbstractActions {

	private static final int ACTION_TIMEOUT_SECONDS = 10;
	private static final Logger LOG = LoggerFactory.getLogger(AbstractActions.class);

	protected final ControlPoint controlPoint;
	protected final RemoteService removeService;

	public AbstractActions (final ControlPoint controlPoint, final RemoteService removeService) {
		this.controlPoint = controlPoint;
		this.removeService = removeService;
		LOG.debug("ServiceType: {}", removeService.getServiceType().getType());
		LOG.debug("Actions: {}", (Object) removeService.getActions());
	}

	protected static void await (final Future<?> f, final String msgFormat, final Object... msgArgs) throws DlnaException {
		try {
			f.get(ACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		}
		catch (final ExecutionException e) {
			throw new DlnaException("Failed to " + String.format(msgFormat, msgArgs), e);
		}
		catch (final TimeoutException e) {
			throw new DlnaTimeoutException("Timed out after " + ACTION_TIMEOUT_SECONDS + "s while trying to " + String.format(msgFormat, msgArgs), e);
		}
		catch (final InterruptedException e) {
			throw new DlnaException("Interupted while trying to " + String.format(msgFormat, msgArgs), e);
		}
	}

	protected static final class Failure {

		private final String actionName;
		private final UpnpResponse response;
		private final String defaultMsg;

		public Failure (final String actionName, final UpnpResponse response, final String defaultMsg) {
			this.actionName = actionName;
			this.response = response;
			this.defaultMsg = defaultMsg;
		}

		public String msg () {
			return String.format("Failed to %s | %s | %s.", this.actionName, this.defaultMsg, this.response);
		}

		public UpnpResponse getResponse () {
			return this.response;
		}

	}

}
