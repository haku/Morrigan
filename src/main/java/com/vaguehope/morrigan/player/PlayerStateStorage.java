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
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.transcode.Transcode;
import com.vaguehope.morrigan.util.StringHelper;

public class PlayerStateStorage {

	private static final int RESTORE_DELAY_SECONDS = 10;
	private static final String DIR_NAME = "playerstate";
	private static final Logger LOG = Logger.getLogger(PlayerStateStorage.class.getName());

	private final Map<ListRef, MediaList> listCache = new ConcurrentHashMap<>();
	private final MediaFactory mf;
	private final ScheduledExecutorService schEx;
	private Config config;

	public PlayerStateStorage (final MediaFactory mf, final ScheduledExecutorService schEx, final Config config) {
		this.mf = mf;
		this.schEx = schEx;
		this.config = config;
	}

	public void writeState (final Player player) {
		try {
			final File outFile = getFile(player.getId());
			final File tmpFile = getTmpFile(outFile);

			final Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8"));
			try {
				w.append(player.getPlaybackOrder().name())
						.append("|").append(String.valueOf(player.getCurrentPosition()))
						.append("|").append(String.valueOf(player.getTranscode().getSymbolicName()))
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

	public void requestReadState (final AbstractPlayer player) {
		this.schEx.schedule(new Runnable() {
			@Override
			public void run () {
				readState(player);
			}
		}, RESTORE_DELAY_SECONDS, TimeUnit.SECONDS);
	}

	protected void readState (final AbstractPlayer player) {
		if (player.isDisposed()) return;

		final File file = getFile(player.getId());
		if (!file.exists()) {
			player.markStateRestoreAttempted();
			return;
		}

		try {
			final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			try {
				final String line = r.readLine();
				if (line == null) return;
				final String[] lineParts = line.split("\\|");

				if (lineParts.length >= 1) {
					final String rawOrder = lineParts[0];
					if (StringHelper.notBlank(rawOrder)) {
						final PlaybackOrder order = PlaybackOrder.parsePlaybackOrderByName(rawOrder);
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

				if (lineParts.length >= 3) {
					final String rawTranscode = lineParts[2];
					if (rawTranscode != null) {
						player.setTranscode(Transcode.parse(rawTranscode.trim()));
					}
				}

				this.listCache.clear();

				final PlayItem currentItem = readPlayItem(readLines(r));
				if (currentItem != null && player.getCurrentItem() == null) player.setCurrentItem(currentItem);

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
		finally {
			player.markStateRestoreAttempted();
		}
	}

	private File getFile (final String playerId) {
		final File dir = new File(this.config.getConfigDir(), DIR_NAME);
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
				w.append(item.getList().getListRef().toUrlForm());
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
				w.append(item.getTrack().getMd5().toString(16)).append("\n");
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

		final String listRefOrPseudoType = lines[0];
		final String trackFilePath = lines[1];
		final String trackMd5 = lines[2];

		if (StringHelper.blank(listRefOrPseudoType)) return null;

		final PlayItemType type = PlayItemType.parse(listRefOrPseudoType);
		if (type != null) return new PlayItem(type);

		final String listRefUrlFrom = listRefOrPseudoType;
		if (StringHelper.blank(trackFilePath) && StringHelper.blank(trackMd5)) return null;

		final ListRef listRef = ListRef.fromUrlForm(listRefUrlFrom);
		MediaList list = this.listCache.get(listRef);
		if (list == null) {
			list = this.mf.getList(listRef);
			if (list == null) {
				LOG.warning("Unknown list: " + listRef);
				return null;
			}
			list.read();
			this.listCache.put(listRef, list);
		}

		MediaItem track = list.getByFile(trackFilePath);
		if (track == null) track = list.getByMd5(new BigInteger(trackMd5, 16));
		if (track != null) return new PlayItem(list, track);

		LOG.warning("Unknown track: " + trackFilePath);
		return null;
	}

}
