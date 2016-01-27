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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.sqlitewrapper.DbException;

public class PlayerStateStorage {

	private static final String DIR_NAME = "playerstate";
	private static final Logger LOG = Logger.getLogger(PlayerStateStorage.class.getName());

	private final Map<String, IMixedMediaDb> listCache = new ConcurrentHashMap<String, IMixedMediaDb>();
	private final MediaFactory mf;

	public PlayerStateStorage (final MediaFactory mf) {
		this.mf = mf;
	}

	public static void writeState (final Player player) {
		try {
			final File outFile = getFile(player.getId());
			final File tmpFile = getTmpFile(outFile);

			final Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8"));
			try {
				w.append(player.getPlaybackOrder().name()).append("\n");
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

	public void readState (final Player player) {
		final File file = getFile(player.getId());
		if (!file.exists()) return;

		try {
			final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			try {
				final String orderRaw = r.readLine();
				final PlaybackOrder order = StringHelper.notBlank(orderRaw) ? OrderHelper.parsePlaybackOrderByName(orderRaw) : null;
				if (order != null) player.setPlaybackOrder(order);

				this.listCache.clear();

				final PlayItem currentItem = readPlayItem(r);
				if (currentItem != null) player.setCurrentItem(currentItem);

				while (true) {
					final PlayItem item = readPlayItem(r);
					if (item == null) break;
					player.getQueue().addToQueue(item);
				}
			}
			finally {
				r.close();
			}
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
		if (item.hasList()) w.append(item.getList().getSerial());
		w.append("\n");
		if (item.hasTrack()) {
			w.append(item.getTrack().getFilepath()).append("\n");
			w.append(item.getTrack().getHashcode().toString(16)).append("\n");
		}
		else {
			w.append("\n");
			w.append("\n");
		}
	}

	private PlayItem readPlayItem (final BufferedReader r) throws IOException, DbException, MorriganException {
		final String listSerial = r.readLine();
		final String trackFilePath = r.readLine();
		final String trackHash = r.readLine();

		if (StringHelper.blank(listSerial)) return null;
		if (StringHelper.blank(trackFilePath) && StringHelper.blank(trackHash)) return null;

		IMixedMediaDb list = this.listCache.get(listSerial);
		if (list == null) {
			list = this.mf.getLocalMixedMediaDbBySerial(listSerial);
			if (list == null) {
				LOG.warning("Unknown list: " + listSerial);
				return null;
			}
			list.read();
			this.listCache.put(listSerial, list);
		}

		IMediaTrack track = list.getByFile(trackFilePath);
		if (track == null) track = list.getByHashcode(new BigInteger(trackHash, 16));
		if (track != null) return new PlayItem(list, track);

		LOG.warning("Unknown track: " + trackFilePath);
		return null;
	}

}
