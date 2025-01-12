package com.vaguehope.morrigan.dlna.players;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.jupnp.model.ModelUtil;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.DIDLObject;
import org.jupnp.support.model.Res;
import org.jupnp.support.model.item.AudioItem;
import org.jupnp.support.model.item.ImageItem;
import org.jupnp.support.model.item.Item;
import org.jupnp.support.model.item.VideoItem;
import org.jupnp.util.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.dlna.MediaFormat;
import com.vaguehope.morrigan.dlna.content.ContentGroup;
import com.vaguehope.morrigan.dlna.content.MediaFileLocator;
import com.vaguehope.morrigan.dlna.httpserver.MediaServer;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.transcode.FfprobeCache;

public class DlnaPlayingParamsFactory {

	private static final Logger LOG = LoggerFactory.getLogger(DlnaPlayingParamsFactory.class);

	private final MediaFileLocator mediaFileLocator;
	private final MediaServer mediaServer;

	public DlnaPlayingParamsFactory(final MediaFileLocator mediaFileLocator, final MediaServer mediaServer) {
		this.mediaFileLocator = mediaFileLocator;
		this.mediaServer = mediaServer;
	}

	public DlnaPlayingParams make(final PlayItem item) throws IOException, DlnaException {
		final File altFile = item.getAltFile();

		final String id;
		if (altFile != null) {
			id = this.mediaFileLocator.fileId(altFile);
		}
		else if (StringHelper.notBlank(item.getItem().getRemoteId())) {
			id = item.getItem().getRemoteId();
		}
		else {
			id = this.mediaFileLocator.fileId(new File(item.getItem().getFilepath()));
		}

		final String uri;
		final MimeType mimeType;
		final long fileSize;
		final int durationSeconds;
		if (altFile != null) {
			uri = this.mediaServer.uriFor(id);
			mimeType = MediaFormat.identify(altFile).toMimeType();
			fileSize = altFile.length();
			durationSeconds = readFileDurationSeconds(altFile);
		}
		else if (item.getItem().hasRemoteLocation()) {
			if (item.hasList()) {
				uri = item.getList().prepairRemoteLocation(item.getItem(), this.mediaServer);
			}
			else {
				uri = item.getItem().getRemoteLocation();
			}
			mimeType = MimeType.valueOf(item.getItem().getMimeType());
			fileSize = item.getItem().getFileSize();
			durationSeconds = item.getItem().getDuration(); // TODO what if this is not available?
		}
		else {
			uri = this.mediaServer.uriFor(id);
			final File file = new File(item.getItem().getFilepath());
			mimeType = MediaFormat.identify(file).toMimeType();
			fileSize = file.length();
			int d = item.getItem().getDuration();
			if (d < 1) d = readFileDurationSeconds(file);
			durationSeconds = d;
		}

		if (durationSeconds < 1) throw new DlnaException("Can not play track without a known duration.");

		final String coverArtUri;
		if (StringHelper.notBlank(item.getItem().getCoverArtRemoteLocation())) {
			coverArtUri = item.getItem().getCoverArtRemoteLocation();
		}
		else {
			final File coverArt = item.getItem().findCoverArt();
			coverArtUri = coverArt != null ? this.mediaServer.uriFor(this.mediaFileLocator.fileId(coverArt)) : null;
		}

		return new DlnaPlayingParams(id, uri, item.getItem().getTitle(), mimeType, fileSize, coverArtUri, durationSeconds);
	}

	/**
	 * Returns valid duration or throws.
	 */
	private static int readFileDurationSeconds (final File file) throws IOException {
		final Long fileDurationMillis = FfprobeCache.INSTANCE.inspect(file).getDurationMillis();
		if (fileDurationMillis == null || fileDurationMillis < 1) throw new IOException("Failed to read file duration: " + file.getAbsolutePath());
		LOG.info("Duration {}ms: {}", fileDurationMillis, file.getAbsolutePath());
		final int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(fileDurationMillis);
		return seconds < 1 ? 1 : seconds; // 0ms < d < 1s gets rounded up to 1s.
	}

	public static class DlnaPlayingParams {

		public final String id;
		public final String uri;
		public final String title;
		public final MimeType mimeType;
		public final long fileSize;
		public final String coverArtUri;
		public final int durationSeconds;

		DlnaPlayingParams(final String id, final String uri, final String title, final MimeType mimeType, final long fileSize, final String coverArtUri, final int durationSeconds) {
			this.id = id;
			this.uri = uri;
			this.title = title;
			this.mimeType = mimeType;
			this.fileSize = fileSize;
			this.coverArtUri = coverArtUri;
			this.durationSeconds = durationSeconds;
		}

		public String asMetadata() {
			return metadataFor(this.id, this.uri, this.title, this.mimeType, this.fileSize, this.coverArtUri, this.durationSeconds);
		}
	}

	// TODO make private.
	public static String metadataFor(final String id, final String uri, final String title, final MimeType mimeType, final long fileSize, final String coverArtUri, final int durationSeconds) {
		if (mimeType == null) return null;
		if (uri == null) throw new IllegalArgumentException("Missing URI.");

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
			// TODO handle this better.
			LOG.info("Failed to generate metedata.", e);
			return null;
		}
	}

}
