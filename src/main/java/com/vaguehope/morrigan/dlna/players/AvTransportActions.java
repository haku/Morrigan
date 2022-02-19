package com.vaguehope.morrigan.dlna.players;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.support.avtransport.callback.GetCurrentTransportActions;
import org.fourthline.cling.support.avtransport.callback.GetMediaInfo;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.TransportAction;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.VideoItem;
import org.seamless.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.dlna.DlnaResponseException;
import com.vaguehope.morrigan.dlna.content.ContentGroup;
import com.vaguehope.morrigan.util.ErrorHelper;

public class AvTransportActions extends AbstractActions {

	private static final Logger LOG = LoggerFactory.getLogger(AvTransportActions.class);

	public AvTransportActions (final ControlPoint controlPoint, final RemoteService avTransportSvc) {
		super(controlPoint, avTransportSvc);
	}

	public void setUri (final String id, final String uri, final String title, final MimeType mimeType, final long fileSize, final String coverArtUri, final int durationSeconds) throws DlnaException {
		final String metadata = metadataFor(id, uri, title, mimeType, fileSize, coverArtUri, durationSeconds);
		final AtomicReference<Failure> err = new AtomicReference<Failure>();
		// SetAVTransportURI() defaults to instanceId=0.
		final Future<?> f = this.controlPoint.execute(new SetAVTransportURI(this.removeService, uri, metadata) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("set av transport URI", response, defaultMsg));
			}
		});
		await(f, "set URI '%s' on transport '%s'.", uri, this.removeService);
		if (err.get() != null) throw new DlnaResponseException(err.get().msg(), err.get().getResponse());
	}

	private static String metadataFor (final String id, final String uri, final String title, final MimeType mimeType, final long fileSize, final String coverArtUri, final int durationSeconds) {
		if (mimeType == null) return null;
		final Res res = new Res(mimeType, Long.valueOf(fileSize), uri);
		if (durationSeconds > 0) res.setDuration(ModelUtil.toTimeString(durationSeconds));
		final Item item;
		switch (ContentGroup.fromMimeType(mimeType)) {
			case VIDEO:
				item = new VideoItem(id, "", title, "", res);
				break;
			case IMAGE:
				item = new ImageItem(id, "", title, "", res);
				break;
			case AUDIO:
				item = new AudioItem(id, "", title, "", res);
				break;
			default:
				return null;
		}
		if (coverArtUri != null) item.addProperty(new DIDLObject.Property.UPNP.ALBUM_ART_URI(URI.create(coverArtUri)));
		final DIDLContent didl = new DIDLContent();
		didl.addItem(item);
		try {
			return new DIDLParser().generate(didl);
		}
		catch (final Exception e) {
			LOG.info("Failed to generate metedata: " + ErrorHelper.getCauseTrace(e));
			return null;
		}
	}

	public void play () throws DlnaException {
		final AtomicReference<Failure> err = new AtomicReference<Failure>();
		final Future<?> f = this.controlPoint.execute(new Play(this.removeService) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("play", response, defaultMsg));
			}
		});
		await(f, "play on transport '%s'.", this.removeService);
		if (err.get() != null) throw new DlnaResponseException(err.get().msg(), err.get().getResponse());
	}

	public void pause () throws DlnaException {
		final AtomicReference<Failure> err = new AtomicReference<Failure>();
		final Future<?> f = this.controlPoint.execute(new Pause(this.removeService) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("pause", response, defaultMsg));
			}
		});
		await(f, "pause playback on transport '%s'.", this.removeService);
		if (err.get() != null) {
			String msg = err.get().msg();
			try {
				final TransportAction[] actions = getTransportActions();
				msg += "  Supported actions: " + Arrays.toString(actions);
			}
			catch (final DlnaException e) {
				// Ignore.
			}
			throw new DlnaResponseException(msg, err.get().getResponse());
		}
	}

	public void stop () throws DlnaException {
		final AtomicReference<Failure> err = new AtomicReference<Failure>();
		final Future<?> f = this.controlPoint.execute(new Stop(this.removeService) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("stop", response, defaultMsg));
			}
		});
		await(f, "stop playback on transport '%s'.", this.removeService);
		if (err.get() != null) throw new DlnaResponseException(err.get().msg(), err.get().getResponse());
	}

	public TransportInfo getTransportInfo () throws DlnaException {
		final AtomicReference<Failure> err = new AtomicReference<Failure>();
		final AtomicReference<TransportInfo> ref = new AtomicReference<TransportInfo>();
		final Future<?> f = this.controlPoint.execute(new GetTransportInfo(this.removeService) {
			@Override
			public void received (final ActionInvocation invocation, final TransportInfo transportInfo) {
				ref.set(transportInfo);
			}

			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("get transport info", response, defaultMsg));
			}
		});
		await(f, "get playback state for transport '%s'.", this.removeService);
		if (ref.get() == null || err.get() != null) throw new DlnaResponseException(err.get().msg(), err.get().getResponse());
		return ref.get();
	}

	public PositionInfo getPositionInfo () throws DlnaException {
		final AtomicReference<Failure> err = new AtomicReference<Failure>();
		final AtomicReference<PositionInfo> ref = new AtomicReference<PositionInfo>();
		final Future<?> f = this.controlPoint.execute(new GetPositionInfo(this.removeService) {
			@Override
			public void received (final ActionInvocation invocation, final PositionInfo positionInfo) {
				ref.set(positionInfo);
			}

			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("get position info", response, defaultMsg));
			}
		});
		await(f, "get position info for transport '%s'.", this.removeService);
		if (ref.get() == null || err.get() != null) throw new DlnaResponseException(err.get().msg(), err.get().getResponse());
		return ref.get();
	}

	public MediaInfo getMediaInfo () throws DlnaException {
		final AtomicReference<Failure> err = new AtomicReference<Failure>();
		final AtomicReference<MediaInfo> ref = new AtomicReference<MediaInfo>();
		final Future<?> f = this.controlPoint.execute(new GetMediaInfo(this.removeService) {
			@Override
			public void received (final ActionInvocation invocation, final MediaInfo mi) {
				ref.set(mi);
			}

			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("get media info", response, defaultMsg));
			}
		});
		await(f, "get media info for transport '%s'.", this.removeService);
		if (ref.get() == null || err.get() != null) throw new DlnaResponseException(err.get().msg(), err.get().getResponse());
		return ref.get();
	}

	public TransportAction[] getTransportActions () throws DlnaException {
		final AtomicReference<Failure> err = new AtomicReference<Failure>();
		final AtomicReference<TransportAction[]> ref = new AtomicReference<TransportAction[]>();
		final Future<?> f = this.controlPoint.execute(new GetCurrentTransportActions(this.removeService) {
			@Override
			public void received (final ActionInvocation invocation, final TransportAction[] actions) {
				ref.set(actions);
			}

			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("get transport actions", response, defaultMsg));
			}
		});
		await(f, "get actions for transport '%s'.", this.removeService);
		if (ref.get() == null || err.get() != null) throw new DlnaResponseException(err.get().msg(), err.get().getResponse());
		return ref.get();
	}

	public void seek (final long seconds) throws DlnaException {
		final String time = ModelUtil.toTimeString(seconds);
		final AtomicReference<Failure> err = new AtomicReference<Failure>();
		final Future<?> f = this.controlPoint.execute(new Seek(this.removeService, time) {
			@Override
			public void failure (final ActionInvocation invocation, final UpnpResponse response, final String defaultMsg) {
				err.set(new Failure("seek to " + time, response, defaultMsg));
			}
		});
		await(f, "seek to %s on transport '%s'.", time, this.removeService);
		if (err.get() != null) throw new DlnaResponseException(err.get().msg(), err.get().getResponse());
	}

}
