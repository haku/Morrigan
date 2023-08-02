package com.vaguehope.morrigan.android.playback;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.provider.DocumentFile;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.checkout.CheckoutIndex;
import com.vaguehope.morrigan.android.checkout.IndexEntry;
import com.vaguehope.morrigan.android.helper.ChecksumHelper;
import com.vaguehope.morrigan.android.helper.ContentHelper;
import com.vaguehope.morrigan.android.helper.ExceptionHelper;
import com.vaguehope.morrigan.android.helper.FileHelper;
import com.vaguehope.morrigan.android.helper.IoHelper;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.playback.MediaDb.Presence;
import com.vaguehope.morrigan.android.playback.MediaDb.SortColumn;
import com.vaguehope.morrigan.android.playback.MediaDb.SortDirection;
import com.vaguehope.morrigan.android.state.Checkout;
import com.vaguehope.morrigan.android.state.ConfigDb;

public class RescanLibrariesService extends MediaBindingAwakeService {

	protected static final LogWrapper LOG = new LogWrapper("RLS");

	private NotificationManager notifMgr;

	public RescanLibrariesService () {
		super("RescanLibrariesService", LOG);
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
			LOG.i("Scan result: %s", result);
			try {
				updateNotifResult(notificationId, notif, result);
				LOG.i("Notification updated.");
			}
			catch (final Exception e) {
				LOG.e("Failed to update notification: ", e);
			}
		}
	}

	private Builder makeNotif () {
		final String title = "Updating Libraries";
		final String subTitle = "Starting...";
		return new NotificationCompat.Builder(this)
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
		this.notifMgr.notify(notificationId, notif.build());
	}

	private void doScans (final int notificationId, final Builder notif) throws IOException {
		ContentHelper.logPermissions(this, LOG);

		final Collection<LibraryMetadata> libraries = getMediaDb().getLibraries();
		for (final LibraryMetadata library : libraries) {
			// TODO check if cancelled.

			LOG.i("Updating library: %s (id=%s)", library.getName(), library.getId());
			scanForNewMedia(library, notificationId, notif);
			updateMetadataForKnowItems(library, notificationId, notif);
			findAndMergeDuplicates(library, notificationId, notif);
			importMetadataFromCheckouts(library, notificationId, notif);
		}
	}

	private void scanForNewMedia (final LibraryMetadata library, final int notificationId, final Builder notif) {
		updateNotifTitle(notificationId, notif, "Updating Library: " + library.getName());
		for (final Uri source : library.getSources()) {
			// TODO check if cancelled.
			scanSourceForNewMedia(source, library, notificationId, notif);
		}
	}

	private void scanSourceForNewMedia (final Uri source, final LibraryMetadata library, final int notificationId, final Builder notif) {
		LOG.i("Scanning source: %s", source);
		updateNotifProgress(notificationId, notif, "Scanning for new files...");

		final MediaDb mediaDb = getMediaDb();

		final List<MediaItem> toAdd = new ArrayList<MediaItem>();
		final List<Long> toMarkAsFound = new ArrayList<Long>();

		final Queue<DocumentFile> dirs = new LinkedList<DocumentFile>();
		final DocumentFile root = FileHelper.dirUriToDocumentFile(this, source);
		if (root.getName() == null) throw new IllegalStateException("Failed to resolve: " + source);
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
					if (!isValidMediaFile(file)) continue;

					final Presence presence = mediaDb.hasMediaUri(library.getId(), file.getUri());
					if (presence == Presence.UNKNOWN) {
						LOG.i("New: %s", file.getUri());
						toAdd.add(makeMediaItem(library, file));
					}
					else if (presence == Presence.MISSING) {
						LOG.i("Restored: %s", file.getUri());
						toMarkAsFound.add(mediaDb.getMediaRowId(library.getId(), file.getUri()));
					}

					if (toAdd.size() > 0) {
						updateNotifProgress(notificationId, notif,
								String.format("Found %s new items, %s restored items...",
										toAdd.size(), toMarkAsFound.size()));
					}
				}
				else {
					LOG.w("Do not know how to read: %s", file.getUri());
				}
			}
		}

		addMediaToLibrary(mediaDb, library, toAdd, toMarkAsFound);
	}

	private static void addMediaToLibrary (final MediaDb mediaDb, final LibraryMetadata library, final List<MediaItem> toAdd, final List<Long> toMarkAsFound) {
		LOG.i("Adding %s new items to library %s...", toAdd.size(), library.getId());
		mediaDb.addMedia(toAdd);

		LOG.i("Marking %s items as found...", toMarkAsFound.size());
		mediaDb.setFilesExist(toMarkAsFound, true);
	}

	private static final Set<String> SUPPORTED_TYPES = new HashSet<String>();
	static {
		SUPPORTED_TYPES.add("application/ogg");
	}

	private static boolean isValidMediaFile (final DocumentFile file) {
		final String type = file.getType();
		if (!SUPPORTED_TYPES.contains(type) && !type.startsWith("audio")) {
			LOG.i("Not audio: %s %s", file.getUri(), type);
			return false;
		}
		if (!file.exists()) {
			LOG.i("Not exists: %s", file.getUri());
			return false;
		}
		if (!file.canRead()) {
			LOG.i("Not readable: %s", file.getUri());
			return false;
		}
		return true;
	}

	private static MediaItem makeMediaItem (final LibraryMetadata library, final DocumentFile file) {
		return new MediaItem(library.getId(),
				file.getUri(), file.getName(), file.length(), file.lastModified(), System.currentTimeMillis());
	}

	private void updateMetadataForKnowItems (final LibraryMetadata library, final int notificationId, final Builder notif) throws IOException {
		updateNotifProgress(notificationId, notif, "Checking for expired metadata...");

		final MediaDb mediaDb = getMediaDb();
		final List<IdUri> itemsToHash = new ArrayList<IdUri>();
		final List<Long> toMarkAsMissing = new ArrayList<Long>();

		final Cursor c = mediaDb.getAllMediaCursor(library.getId(), SortColumn.PATH, SortDirection.ASC);
		try {
			if (c != null && c.moveToFirst()) {
				final MediaCursorReader reader = new MediaCursorReader();
				do {
					// TODO check if cancelled.

					final long id = reader.readId(c);
					final Uri uri = reader.readUri(c);
					final DocumentFile file = FileHelper.fileUriToDocumentFile(this, uri);
					if (file.exists()) {
						final BigInteger libFileHash = reader.readFileHash(c);
						final long libSizeBytes = reader.readSizeBytes(c);
						final long libLastModified = reader.readFileLastModified(c);
						if (libFileHash == null || libSizeBytes != file.length() || libLastModified != file.lastModified()) {
							itemsToHash.add(new IdUri(id, uri));
						}
					}
					else {
						toMarkAsMissing.add(id);
					}
				}
				while (c.moveToNext());
			}
		}
		finally {
			IoHelper.closeQuietly(c);
		}

		LOG.i("Marking files missing: %s", toMarkAsMissing.size());
		mediaDb.setFilesExist(toMarkAsMissing, false);

		LOG.i("Checksums to calculate: %s", itemsToHash.size());
		updateNotifProgress(notificationId, notif, "Calculating checksums...");
		int i = 0;
		final byte[] buffer = ChecksumHelper.createBuffer();
		for (final IdUri item : itemsToHash) {
			// TODO check if cancelled.

			updateFileHash(item.id, item.uri, buffer);

			i += 1;
			updateNotifProgress(notificationId, notif,
					"Calculated " + i + " of " + itemsToHash.size() + " checksums...",
					itemsToHash.size(), i);
		}
	}

	private void findAndMergeDuplicates (final LibraryMetadata library, final int notificationId, final Builder notif) throws IOException {
		LOG.i("Finding duplicates...");
		final MediaDb mediaDb = getMediaDb();
		final Cursor c = mediaDb.findDuplicates(library.getId());
		try {
			if (c != null && c.moveToFirst()) {
				final MediaCursorReader reader = new MediaCursorReader();
				Group group = new Group();
				do {
					// TODO check if cancelled.

					final long id = reader.readId(c);
					final Uri uri = reader.readUri(c);
					final IdUri item = new IdUri(id, uri);

					final BigInteger hash = reader.readFileHash(c);

					final Presence missing = reader.readMissing(c);
					final boolean exists = missing != Presence.MISSING; // Default to exists.

					if (group.canAddHash(hash)) {
						group.add(item, hash, exists);
					}
					else {
						resolveDuplicateGroup(group);
						group = new Group();
						group.add(item, hash, exists);
					}
				}
				while (c.moveToNext());

				// Do not forget the last one.
				if (group.size() > 0) {
					resolveDuplicateGroup(group);
				}
			}
		}
		finally {
			IoHelper.closeQuietly(c);
		}
	}

	private void resolveDuplicateGroup (final Group group) {
		if (group.size() < 2) {
			throw new IllegalStateException("Duplicate group has only one item: " + group);
		}

		if (group.existantCount() > 1) {
			LOG.i("Multiple existant: %s", group.existant());
			return;
		}

		if (group.existantCount() < 1) {
			LOG.d("None existant: %s", group.missing());
			return;
		}

		if (group.existantCount() != 1) {
			throw new IllegalStateException("Group should only contain one existant: " + group);
		}
		if (group.missingCount() < 1) {
			throw new IllegalStateException("Group should contain at least one missing: " + group);
		}

		final IdUri existant = group.existant().get(0);
		LOG.d("Mergeble: %s << %s", existant, group.missing());

		final MediaDb mediaDb = getMediaDb();
		final long destRowId = existant.id;
		final Collection<Long> fromRowIds = new ArrayList<Long>();
		for (final IdUri m : group.missing()) {
			fromRowIds.add(m.id);
		}

		LOG.i("Merging %s << %s...", destRowId, fromRowIds);
		mediaDb.mergeItems(destRowId, fromRowIds);
	}

	private void importMetadataFromCheckouts (final LibraryMetadata library, final int notificationId, final Builder notif) throws IOException {
		LOG.i("Importing metadata from checkouts...");
		updateNotifProgress(notificationId, notif, "Importing from checkouts...");

		final MediaDb mediaDb = getMediaDb();
		final ConfigDb configDb = new ConfigDb(this);

		final Map<Long, BigInteger> originalHashesToAdd = new HashMap<Long, BigInteger>();
		final Map<Long, Long> durationSecondsToAdd = new HashMap<Long, Long>();
		final Map<Long, Long> timeAddedToAdd = new HashMap<Long, Long>();
		final Map<Long, Collection<MediaTag>> tagsToAppend = new HashMap<Long, Collection<MediaTag>>();

		for (final Checkout checkout : configDb.getCheckouts()) {
			final Set<IndexEntry> index = CheckoutIndex.read(this, checkout);
			for (final IndexEntry entry : index) {
				if (entry.getHash() != null) {
					final long[] mfRowIds = mediaDb.getMediaRowIds(library.getId(), entry.getHash());
					for (final long mfRowId : mfRowIds) {
						if (entry.getOHash() != null) {
							originalHashesToAdd.put(mfRowId, entry.getOHash());
						}
						if (entry.getDurationSeconds() > 0) {
							durationSecondsToAdd.put(mfRowId, entry.getDurationSeconds());
						}
						if (entry.getTimeAdded() > 0) {
							timeAddedToAdd.put(mfRowId, entry.getTimeAdded());
						}
						if (entry.hasTags()) {
							tagsToAppend.put(mfRowId, entry.getTags());
						}
					}
				}
				else {
					LOG.w("Checkout index entry missing hash: %s", entry.getLocalPath());
				}
			}
		}

		long startTime;

		LOG.i("Appending original hashes to %s items...", originalHashesToAdd.size());
		updateNotifProgress(notificationId, notif, "Appending original hashes...");
		startTime = System.currentTimeMillis();
		mediaDb.updateOriginalHashes(originalHashesToAdd);
		LOG.i("Append original hashes to %s items in %sms.", originalHashesToAdd.size(), System.currentTimeMillis() - startTime);

		LOG.i("Appending durations to %s items...", durationSecondsToAdd.size());
		updateNotifProgress(notificationId, notif, "Appending durations...");
		startTime = System.currentTimeMillis();
		mediaDb.updateDurationSeconds(durationSecondsToAdd);
		LOG.i("Append durations to %s items in %sms.", durationSecondsToAdd.size(), System.currentTimeMillis() - startTime);

		LOG.i("Appending time added to %s items...", timeAddedToAdd.size());
		updateNotifProgress(notificationId, notif, "Appending time added...");
		startTime = System.currentTimeMillis();
		mediaDb.updateTimeAdded(timeAddedToAdd);
		LOG.i("Append time added to %s items in %sms.", timeAddedToAdd.size(), System.currentTimeMillis() - startTime);

		LOG.i("Appending tags to %s items...", tagsToAppend.size());
		updateNotifProgress(notificationId, notif, "Appending tags...");
		startTime = System.currentTimeMillis();
		mediaDb.appendTags(tagsToAppend);
		LOG.i("Append tags to %s items in %sms.", tagsToAppend.size(), System.currentTimeMillis() - startTime);
	}

	private static final class IdUri {
		final long id;
		final Uri uri;

		public IdUri (final long id, final Uri uri) {
			if (id < 0) throw new IllegalArgumentException("id must be >= 0.");
			if (uri == null) throw new IllegalArgumentException("uri must not be null.");
			this.id = id;
			this.uri = uri;
		}

		@Override
		public String toString () {
			return String.format("{%s: %s}", this.id, this.uri);
		}
	}

	private static final class Group {
		private BigInteger groupHash;
		private final List<IdUri> existant = new ArrayList<IdUri>();
		private final List<IdUri> missing = new ArrayList<IdUri>();

		public boolean canAddHash(final BigInteger hash) {
			if (hash == null) throw new IllegalArgumentException("hash can not be null.");
			return this.groupHash == null || this.groupHash.equals(hash);
		}

		public void add(final IdUri item, final BigInteger hash, final boolean exists) {
			if (item == null) throw new IllegalArgumentException("item can not be null.");
			if (hash == null) throw new IllegalArgumentException("hash can not be null.");

			if (this.groupHash != null) {
				if (!this.groupHash.equals(hash)) {
					throw new IllegalArgumentException("Can not add item with different hash.");
				}
			}
			else {
				this.groupHash = hash;
			}

			if (exists) {
				this.existant.add(item);
			}
			else {
				this.missing.add(item);
			}
		}

		public int size () {
			return this.existant.size() + this.missing.size();
		}

		public int existantCount () {
			return this.existant.size();
		}

		public int missingCount () {
			return this.missing.size();
		}

		public List<IdUri> existant () {
			return this.existant;
		}

		public List<IdUri> missing () {
			return this.missing;
		}

		@Override
		public String toString () {
			return String.format("Group{%s, %s}", this.existant, this.missing);
		}

	}

	private void updateFileHash (final long id, final Uri uri, final byte[] buffer) throws IOException {
		final InputStream is = getContentResolver().openInputStream(uri);
		try {
			final BigInteger hash = ChecksumHelper.generateMd5Checksum(is, buffer);
			LOG.i("%s %s", hash.toString(16), uri);
			final DocumentFile file = FileHelper.fileUriToDocumentFile(this, uri);
			getMediaDb().setFileMetadata(id, file.length(), file.lastModified(), hash);
		}
		finally {
			IoHelper.closeQuietly(is);
		}
	}

}
