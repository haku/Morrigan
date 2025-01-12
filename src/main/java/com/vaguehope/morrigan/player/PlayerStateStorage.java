package com.vaguehope.morrigan.player;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.player.PlayerState.QueueItem;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.util.ExceptionHelper;
import com.vaguehope.morrigan.util.StringHelper;

public class PlayerStateStorage {

	private static final int RESTORE_DELAY_SECONDS = 10;
	private static final String DIR_NAME = "playerstate";
	private static final Logger LOG = LoggerFactory.getLogger(PlayerStateStorage.class);

	private final Gson gson = new GsonBuilder().create();
	private final MediaFactory mf;
	private final ScheduledExecutorService schEx;
	private final Config config;

	public PlayerStateStorage(final MediaFactory mf, final ScheduledExecutorService schEx, final Config config) {
		this.mf = mf;
		this.schEx = schEx;
		this.config = config;
	}

	public void writeState(final Player player) {
		final List<QueueItem> queue = player.getQueue().getQueueList().stream().map(i -> toQueueItem(i)).collect(Collectors.toList());
		final PlayerState playerState = new PlayerState(
				player.getPlaybackOrder(),
				player.getTranscode(),
				player.getCurrentPosition(),
				player.getCurrentList() != null ? player.getCurrentList().getListRef().toUrlForm() : null,
				toQueueItem(player.getCurrentItem()),
				queue);
		final String json = this.gson.toJson(playerState);

		final File outFile = getFile(player.getId());
		final File tmpFile = getTmpFile(outFile);
		try {
			FileUtils.writeStringToFile(tmpFile, json, StandardCharsets.UTF_8);
			if (!tmpFile.renameTo(outFile)) {
				LOG.error("Failed to mv {} to {}.", tmpFile.getAbsolutePath(), outFile.getAbsolutePath());
			}
		}
		catch (final Exception e) {
			LOG.error("Failed to write state for player " + player.getId(), e);
		}
	}

	public void requestReadState(final Player player) {
		this.schEx.schedule(new Runnable() {
			@Override
			public void run() {
				readState(player);
			}
		}, RESTORE_DELAY_SECONDS, TimeUnit.SECONDS);
	}

	protected void readState(final Player player) {
		if (player.isDisposed()) return;

		final File file = getFile(player.getId());
		if (!file.exists()) {
			player.markStateRestoreAttempted();
			return;
		}

		try {
			final String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
			final PlayerState state = this.gson.fromJson(json, PlayerState.class);
			if (state.playbackOrder != null) player.setPlaybackOrder(state.playbackOrder);
			if (state.transcode != null) player.setTranscode(state.transcode);
			if (state.position > 0) LOG.info("TODO restore position: " + state.position);

			final Map<ListRef, MediaList> listCache = new HashMap<>();
			final MediaList list = getList(ListRef.fromUrlForm(state.listRef), listCache);
			if (list != null) player.setCurrentList(list);

			final PlayItem currentItem = fromQueueItem(state.item, listCache);
			if (currentItem != null && player.getCurrentItem() == null) player.setCurrentItem(currentItem);

			if (state.queue != null) {
				for (final QueueItem qi : state.queue) {
					final PlayItem pi = fromQueueItem(qi, listCache);
					if (pi != null) player.getQueue().addToQueue(pi);
				}
			}

			LOG.info("Restorted state for player {}.", player.getId());
		}
		catch (final Exception e) {
			LOG.warn("Failed to read state for player: {}", player.getId(), e);
		}
		finally {
			player.markStateRestoreAttempted();
		}
	}

	private static QueueItem toQueueItem(final PlayItem i) {
		if (i == null) return null;

		if (i.getType().isPseudo()) {
			return new QueueItem(i.getType().name(), null, null, null, null);
		}

		final ListRef listRef = i.getListRef();
		final BigInteger md5 = i.getMd5();
		return new QueueItem(
				listRef != null ? listRef.toUrlForm() : null,
				i.getFilepath(),
				i.getRemoteId(),
				md5 != null ? md5.toString(16) : "0",
				i.resolveTitle(null));
	}

	private PlayItem fromQueueItem(final QueueItem i, final Map<ListRef, MediaList> listCache) throws DbException, MorriganException {
		if (i == null) return null;
		if (StringHelper.blank(i.listRef)) return null;

		final PlayItemType type = PlayItemType.parse(i.listRef);
		if (type != null) return PlayItem.makeAction(type);

		if (StringHelper.blank(i.filepath) && StringHelper.blank(i.md5)) return null;

		final ListRef listRef = ListRef.fromUrlForm(i.listRef);
		final PlayItem unresolved = PlayItem.makeUnresolved(listRef, i.filepath, i.remoteId, new BigInteger(i.md5, 16), i.title);
		try {
			final PlayItem ready = unresolved.makeReady((r) -> getList(r, listCache));
			if (ready != null) return ready;
		}
		catch (final MorriganException e) {}
		return unresolved;
	}

	private MediaList getList(final ListRef listRef, final Map<ListRef, MediaList> listCache) {
		MediaList list = null;
		if (listCache.containsKey(listRef)) {
			list = listCache.get(listRef);
		}
		else {
			try {
				list = this.mf.getList(listRef);
				if (list != null) {
					list.read();
				}
				else {
					LOG.warn("Can not restore reference to unknown list: {}", listRef);
				}
			}
			catch (final MorriganException e) {
				LOG.warn("Failed to restore reference to list {}: {}", listRef, ExceptionHelper.causeTrace(e));
			}
			listCache.put(listRef, list);
		}
		return list;
	}

	private File getFile(final String playerId) {
		final File dir = new File(this.config.getConfigDir(), DIR_NAME);
		if (!dir.exists()) dir.mkdirs();
		return new File(dir, playerId);
	}

	private static File getTmpFile(final File outFile) {
		return new File(outFile.getAbsolutePath() + ".tmp");
	}

}
