package com.vaguehope.morrigan.player;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaListReference.MediaListType;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.sqlitewrapper.DbException;

public class PlayerStateStorage {

	private static final int RESTORE_DELAY_SECONDS = 10;
	private static final String DIR_NAME = "playerstate";
	private static final Logger LOG = Logger.getLogger(PlayerStateStorage.class.getName());

	private final Map<String, IMixedMediaDb> listCache = new ConcurrentHashMap<String, IMixedMediaDb>();
	private final MediaFactory mf;
	private final ScheduledExecutorService schEx;

	public PlayerStateStorage (final MediaFactory mf, final ScheduledExecutorService schEx) {
		this.mf = mf;
		this.schEx = schEx;
	}

	public static void writeState (final Player player) {
		try {
			final File outFile = getFile(player.getId());
			final File tmpFile = getTmpFile(outFile);

			final Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8"));
			try {
				w.append(player.getPlaybackOrder().name())
						.append("|").append(String.valueOf(player.getCurrentPosition()))
						.append("\n");
				appendPlayItem(w, player.getCurrentItem());
				for (final PlayItem item : player.getQueue().getQueueList()) {
					appendPlayItem(w, item);
				}
			}
			finally {
				w.close();
			}

			if (!tmpFile.renameTo(outFile)) {
				LOG.warning(String.format("Failed to mv %s to %s.", tmpFile.getAbsolutePath(), outFile.getAbsolutePath()));
			}
		}
		catch (final Exception e) {
			LOG.log(Level.WARNING, "Failed to write state for player " + player.getId(), e);
		}

	}

	public void requestReadState (final Player player) {
		this.schEx.schedule(new Runnable() {
			@Override
			public void run () {
				readState(player);
			}
		}, RESTORE_DELAY_SECONDS, TimeUnit.SECONDS);
	}

	protected void readState (final Player player) {
		if (player.isDisposed()) return;

		final File file = getFile(player.getId());
		if (!file.exists()) return;

		try {
			final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			try {
				final String line = r.readLine();
				if (line == null) return;
				final String[] lineParts = line.split("\\|");

				if (lineParts.length >= 1) {
					final String rawOrder = lineParts[0];
					if (StringHelper.notBlank(rawOrder)) {
						final PlaybackOrder order = OrderHelper.parsePlaybackOrderByName(rawOrder);
						if (order != null) player.setPlaybackOrder(order);
					}
				}

				if (lineParts.length >= 2) {
					final String rawPosition = lineParts[1];
					if (StringHelper.notBlank(rawPosition)) {
						final long position = Long.parseLong(rawPosition);
						if (position > 0) {
							LOG.info("TODO restore position: " + position);
						}
					}
				}

				this.listCache.clear();

				final PlayItem currentItem = readPlayItem(readLines(r));
				if (currentItem != null) player.setCurrentItem(currentItem);

				while (true) {
					final String[] lines = readLines(r);
					if (lines == null) break;
					final PlayItem item = readPlayItem(lines);
					if (item != null) player.getQueue().addToQueue(item);
				}
			}
			finally {
				r.close();
			}
			LOG.info("Restorted state for player " + player.getId() + ".");
		}
		catch (final Exception e) {
			LOG.log(Level.WARNING, "Failed to read state for player " + player.getId(), e);
		}
	}

	private static File getFile (final String playerId) {
		final File dir = new File(new File(Config.getConfigDir()), DIR_NAME);
		if (!dir.exists()) dir.mkdirs();
		return new File(dir, playerId);
	}

	private static File getTmpFile (final File outFile) {
		return new File(outFile.getAbsolutePath() + ".tmp");
	}

	private static void appendPlayItem (final Writer w, final PlayItem item) throws IOException {
		if (item == null) {
			w.append("\n");
			w.append("\n");
			w.append("\n");
		}
		else if (item.getType().isPseudo()) {
			w.append(item.getType().name()).append("\n");
			w.append("\n");
			w.append("\n");
		}
		else {
			if (item.hasList()) {
				w.append(item.getList().getType()).append("|").append(item.getList().getSerial());
			}
			w.append("\n");
			if (item.hasTrack()) {
				if (StringHelper.notBlank(item.getTrack().getFilepath())) {
					w.append(item.getTrack().getFilepath());
				}
				else if (StringHelper.notBlank(item.getTrack().getRemoteId())) {
					w.append(item.getTrack().getRemoteId());
				}
				w.append("\n");
				w.append(item.getTrack().getHashcode().toString(16)).append("\n");
			}
			else {
				w.append("\n");
				w.append("\n");
			}
		}
	}

	private static String[] readLines (final BufferedReader r) throws IOException {
		final String[] lines = new String[3];
		for (int i = 0; i < 3; i++) {
			lines[i] = r.readLine();
			if (lines[i] == null) return null;
		}
		return lines;
	}

	private PlayItem readPlayItem (final String[] lines) throws DbException, MorriganException {
		if (lines == null) return null;

		final String listTypeAndSerialOrPseudoType = lines[0];
		final String trackFilePath = lines[1];
		final String trackHash = lines[2];

		if (StringHelper.blank(listTypeAndSerialOrPseudoType)) return null;

		final PlayItemType type = PlayItemType.parse(listTypeAndSerialOrPseudoType);
		if (type != null) return new PlayItem(type);

		final String listTypeAndSerial = listTypeAndSerialOrPseudoType;
		if (StringHelper.blank(trackFilePath) && StringHelper.blank(trackHash)) return null;

		final MediaListType listType;
		final String listSerial;
		if (listTypeAndSerial.contains("|")) {
			final String[] split = listTypeAndSerial.split("\\|", 2);
			listType = MediaListType.valueOf(split[0]);
			listSerial = split[1];
		}
		else {
			listType = MediaListType.LOCALMMDB;
			listSerial = listTypeAndSerial;
		}

		IMixedMediaDb list = this.listCache.get(listSerial);
		if (list == null) {
			switch (listType) {
				case LOCALMMDB:
					list = this.mf.getLocalMixedMediaDbBySerial(listSerial);
					if (list == null) {
						LOG.warning("Unknown list: " + listSerial);
						return null;
					}
					list.read();
					this.listCache.put(listSerial, list);
					break;
				case EXTMMDB:
					list = this.mf.getExternalDb(listSerial);
					if (list == null) {
						LOG.warning("Unknown list: " + listSerial);
						return null;
					}
					this.listCache.put(listSerial, list);
					break;
				default:
					LOG.warning("Unsupported list type: " + listType);
					return null;
			}
		}

		IMediaTrack track = list.getByFile(trackFilePath);
		if (track == null) track = list.getByHashcode(new BigInteger(trackHash, 16));
		if (track != null) return new PlayItem(list, track);

		LOG.warning("Unknown track: " + trackFilePath);
		return null;
	}

}
