package morrigan.dlna.players;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableAllowedValueRange;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.support.renderingcontrol.callback.GetVolume;
import org.jupnp.support.renderingcontrol.callback.SetVolume;

import morrigan.dlna.DlnaException;
import morrigan.dlna.DlnaResponseException;

/**
 * ListPresets
 * SelectPreset
 * GetMute
 * SetMute
 * GetVolume
 * SetVolume
 */
public class RenderingControlActions extends AbstractActions {

	private final Integer volumeMaxValue;

	public RenderingControlActions (final ControlPoint controlPoint, final RemoteService removeService) {
		super(controlPoint, removeService);
		this.volumeMaxValue = readVolumeMaxValue(removeService);
	}

	private static Integer readVolumeMaxValue (final RemoteService removeService) {
		final StateVariable<RemoteService> var = removeService.getStateVariable("Volume");
		if (var == null) return null;

		final StateVariableTypeDetails typeDetails = var.getTypeDetails();
		if (typeDetails == null) return null;

		final StateVariableAllowedValueRange range = typeDetails.getAllowedValueRange();
		if (range == null) return null;

		return (int) range.getMaximum();
	}

	public Integer getVolumeMaxValue () {
		return this.volumeMaxValue;
	}

	public int getVolume () throws DlnaException {
		final AtomicReference<Failure> err = new AtomicReference<>();
		final AtomicReference<Integer> ref = new AtomicReference<>();
		final Future<?> f = this.controlPoint.execute(new GetVolume(this.removeService) {

			@Override
			public void received (final ActionInvocation<?> invocation, final int volume) {
				ref.set(volume);
			}

			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("get volume", response, defaultMsg));
			}
		});
		await(f, "get volume for renderer '%s'.", this.removeService);
		if (ref.get() == null || err.get() != null) throw new DlnaResponseException(err.get().msg(), err.get().getResponse());
		return ref.get();
	}

	public void setVolume (final int newVolume) throws DlnaException {
		final AtomicReference<Failure> err = new AtomicReference<>();
		final Future<?> f = this.controlPoint.execute(new SetVolume(this.removeService, newVolume) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("set volume", response, defaultMsg));
			}
		});
		await(f, "set volume for renderer '%s'.", this.removeService);
		if (err.get() != null) throw new DlnaResponseException(err.get().msg(), err.get().getResponse());
	}

}
