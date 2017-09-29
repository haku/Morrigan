package com.vaguehope.morrigan.android.playback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.ExceptionHelper;
import com.vaguehope.morrigan.android.helper.LogWrapper;

public class RescanDbsService extends MediaBindingAwakeService {

	protected static final LogWrapper LOG = new LogWrapper("RDS");

	private NotificationManager notifMgr;

	public RescanDbsService () {
		super("RescanDbsService", LOG);
	}

	@Override
	public void onCreate () {
		super.onCreate();
		this.notifMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	protected void doWork (final Intent i) {
		final int notificationId = (int) (System.currentTimeMillis()); // Probably unique.
		final Builder notif = makeNotif();
		updateNotif(notificationId, notif);

		String result = "Unknown result.";
		try {
			waitForDbReady();
			doScans(notificationId, notif);
			result = "Finished.";
		}
		catch (final Exception e) {
			LOG.e("Scan failed.", e);
			result = ExceptionHelper.veryShortMessage(e);
		}
		finally {
			updateNotifResult(notificationId, notif, result);
		}
	}

	private Builder makeNotif () {
		final String title = "Scanning Media";
		final String subTitle = "Scan starting...";
		return new Notification.Builder(this)
				.setSmallIcon(R.drawable.search)
				.setContentTitle(title)
				.setContentText(subTitle)
				.setTicker(subTitle)
				.setOngoing(true);
	}

	private void updateNotifTitle (final int notificationId, final Builder notif, final String msg) {
		updateNotif(notificationId, notif.setContentTitle(msg));
	}

	private void updateNotifProgress (final int notificationId, final Builder notif, final String msg) {
		updateNotif(notificationId, notif.setContentText(msg));
	}

	protected void updateNotifProgress (final int notificationId, final Builder notif, final String msg, final int max, final int progress) {
		updateNotif(notificationId, notif.setContentText(msg).setProgress(max, progress, false));
	}

	private void updateNotifResult (final int notificationId, final Builder notif, final String msg) {
		updateNotif(notificationId, notif.setOngoing(false)
				.setProgress(0, 0, false)
				.setContentText(msg));
	}

	private void updateNotif (final int notificationId, final Builder notif) {
		this.notifMgr.notify(notificationId, notif.getNotification());
	}

	private void doScans (final int notificationId, final Builder notif) {
		final Collection<DbMetadata> dbs = getMediaDb().getDbs();
		for (final DbMetadata db : dbs) {
			// TODO check if cancelled.

			scanDb(db, notificationId, notif);
		}
	}

	private void scanDb (final DbMetadata db, final int notificationId, final Builder notif) {
		updateNotifTitle(notificationId, notif, "Scanning " + db.getName());

		for (final Uri source : db.getSources()) {
			// TODO check if cancelled.

			scanSourceForNewMedia(source, db, notificationId, notif);
		}

		// TODO scan and verify existing media items, update metadata, etc.
	}

	private void scanSourceForNewMedia (final Uri source, final DbMetadata db, final int notificationId, final Builder notif) {
		LOG.i("Scanning: %s", source);
		final MediaDb mediaDb = getMediaDb();

		final List<MediaItem> mediaToAddToDb = new ArrayList<MediaItem>();
		final Queue<DocumentFile> dirs = new LinkedList<DocumentFile>();
		int newItemCount = 0;

		updateNotifProgress(notificationId, notif, "Scanning for new files...");

		final DocumentFile root = DocumentFile.fromTreeUri(this, source);
		dirs.add(root);

		while (!dirs.isEmpty()) {
			// TODO check if cancelled.

			final DocumentFile dir = dirs.poll();
			final DocumentFile[] files = dir.listFiles();
			if (files == null) {
				LOG.w("Can not list files: %s", dir.getUri());
				continue;
			}

			for (final DocumentFile file : files) {
				// TODO check if cancelled.

				if (file.isDirectory()) {
					dirs.add(file);
				}
				else if (file.isFile()) {
					final MediaItem newItem = makeNewMediaItem(mediaDb, file);
					if (newItem != null) {
						LOG.i("New file: %s", file.getUri());

						mediaToAddToDb.add(newItem);
						if (mediaToAddToDb.size() >= 100) addMediaToDb(mediaDb, db, mediaToAddToDb);

						newItemCount += 1;
						updateNotifProgress(notificationId, notif, "Found " + newItemCount + " new items");
					}
				}
				else {
					LOG.w("Do not know how to read: %s", file.getUri());
				}
			}
		}
		addMediaToDb(mediaDb, db, mediaToAddToDb);
		LOG.i("Total items added: %s", newItemCount);
	}

	private static void addMediaToDb (final MediaDb mediaDb, final DbMetadata db, final List<MediaItem> items) {
		if (items.size() > 0) {
			LOG.i("Adding %s items to DB %s...", items.size(), db.getId());
			mediaDb.addMedia(db.getId(), items);
		}
		items.clear();
	}

	private static final Set<String> SUPPORTED_TYPES = new HashSet<String>();
	static {
		SUPPORTED_TYPES.add("application/ogg");
	}

	/**
	 * Returns null if already present in DB or not a supported media file.
	 */
	private static MediaItem makeNewMediaItem (final MediaDb mediaDb, final DocumentFile file) {
		final String type = file.getType();
		if (!SUPPORTED_TYPES.contains(type) && !type.startsWith("audio")) {
			LOG.i("Not audio: %s %s", file.getUri(), type);
			return null;
		}
		if (!file.exists()) {
			LOG.i("Not exists: %s", file.getUri());
			return null;
		}
		if (!file.canRead()) {
			LOG.i("Not readable: %s", file.getUri());
			return null;
		}
		if (mediaDb.hasMediaUri(file.getUri())) return null;
		return new MediaItem(file.getUri(), file.getName(), file.length(), file.lastModified(), System.currentTimeMillis());
	}

}
