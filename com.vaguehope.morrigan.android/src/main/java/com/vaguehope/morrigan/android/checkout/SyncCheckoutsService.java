package com.vaguehope.morrigan.android.checkout;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.vaguehope.morrigan.android.AwakeService;
import com.vaguehope.morrigan.android.C;
import com.vaguehope.morrigan.android.MnApi;
import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.DialogHelper.Listener;
import com.vaguehope.morrigan.android.helper.ExceptionHelper;
import com.vaguehope.morrigan.android.helper.FileHelper;
import com.vaguehope.morrigan.android.helper.FormaterHelper;
import com.vaguehope.morrigan.android.helper.HttpFileDownloadHandler.DownloadProgressListener;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.MlistItemList;
import com.vaguehope.morrigan.android.model.MlistState;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.state.Checkout;
import com.vaguehope.morrigan.android.state.ConfigDb;

public class SyncCheckoutsService extends AwakeService {

	public static final String EXTRA_HOST_SYNC = C.PACKAGE_PREFIX + "host_to_sync";

	private static final LogWrapper LOG = new LogWrapper("SCS");

	private ConfigDb configDb;
	private NotificationManager notifMgr;

	public SyncCheckoutsService () {
		super("SyncCheckoutsService");
	}

	@Override
	public void onCreate () {
		super.onCreate();
		this.configDb = new ConfigDb(this);
	}

	@Override
	protected void doWork (final Intent i) {
		this.notifMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		final int notificationId = (int) (System.currentTimeMillis()); // Probably unique.
		final Builder notif = makeNotif(notificationId);
		String result = "Unknown result.";
		try {
			final String hostToSync = i.getExtras() != null ? i.getExtras().getString(EXTRA_HOST_SYNC) : null;
			doSyncs(notificationId, notif, hostToSync);
			result = "Finished.";
		}
		catch (final Exception e) {
			LOG.e("Sync failed.", e);
			result = ExceptionHelper.veryShortMessage(e);
		}
		finally {
			updateNotifResult(notificationId, notif, result);
		}
	}

	private Builder makeNotif (final int notificationId) {
		final String title = "Syncing Media";
		final String subTitle = "Sync starting...";
		final Builder notif = new Notification.Builder(this)
				.setSmallIcon(R.drawable.pref)
				.setContentTitle(title)
				.setContentText(subTitle)
				.setTicker(subTitle)
				.setOngoing(true);
		updateNotif(notificationId, notif);
		return notif;
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

	private void doSyncs (final int notificationId, final Builder notif, final String hostId) throws IOException {
		final List<Checkout> checkouts = this.configDb.getCheckouts();
		for (final Checkout checkout : checkouts) {
			if (hostId != null && !hostId.equals(checkout.getHostId())) continue;

			try {
				syncCheckout(checkout, notificationId, notif);
			}
			catch (final Exception e) {
				this.configDb.updateCheckout(this.configDb.getCheckout(checkout.getId()).withStatus(e.toString()));

				if (e instanceof RuntimeException) throw (RuntimeException) e;
				if (e instanceof IOException) throw (IOException) e;
				throw new IllegalStateException(e.toString(), e);
			}
		}
		LOG.i("Finished syncing %s checkouts.", checkouts.size());
	}

	private void syncCheckout (final Checkout checkout, final int notificationId, final Builder notif) throws IOException {
		LOG.i("Syncing CO %s: %s", checkout.getId(), checkout.getQuery());
		updateNotifTitle(notificationId, notif, checkout.getQuery());

		final File localDir = new File(checkout.getLocalDir());
		if (!localDir.exists() && !localDir.mkdirs()) throw new IOException("Failed to create '" + localDir.getAbsolutePath() + "'.");

		final ServerReference host = this.configDb.getServer(checkout.getHostId());
		updateNotifProgress(notificationId, notif, "Fetching list of items...");
		List<? extends MlistItem> items = fetchListOfItems(checkout, host);
		LOG.i("Fetched list of %s items.", items.size());
		updateNotifProgress(notificationId, notif, String.format("Checking %s items...", items.size()));

		final MlistState dbSrcs = MnApi.fetchDbSrcs(host, checkout.getDbRelativePath());
		final List<String> srcs = dbSrcs.getSrcs();
		List<ItemAndFile> allItemsAndLocalFiles = computeLocalFiles(localDir, items, srcs);
		List<ItemAndFile> toCopy = computeRequireDownloading(allItemsAndLocalFiles);
		LOG.i("Identified %s of %s items to download.", toCopy.size(), allItemsAndLocalFiles.size());

		final int transcodedCount = requestTranscodes(checkout, notificationId, notif, host, toCopy);
		if (transcodedCount > 0) {
			LOG.i("Transcoded %s items.", transcodedCount);
			updateNotifProgress(notificationId, notif, "Refreshing list of items...");
			items = fetchListOfItems(checkout, host);
			LOG.i("Re-fetched list of %s items.", items.size());
			allItemsAndLocalFiles = computeLocalFiles(localDir, items, srcs);
			toCopy = computeRequireDownloading(allItemsAndLocalFiles);
			LOG.i("Identified %s of %s items to download.", toCopy.size(), allItemsAndLocalFiles.size());
		}

		CheckoutIndex.write(this, checkout, allItemsAndLocalFiles);

		final long spaceAfterCopy = localDir.getFreeSpace() - totalTransferSize(toCopy);
		if (spaceAfterCopy < 1) throw new IOException(String.format(
				"Not enough space on device: %s short.",
				FormaterHelper.readableFileSize(Math.abs(spaceAfterCopy))));

		final SyncDlPrgListnr prgLstnr = downloadFiles(checkout, notificationId, notif, host, toCopy);
		LOG.i("Downloaded %s items (%s bytes).", prgLstnr.getTransferedItems(), prgLstnr.getTransferedItemBytes());
		final List<File> toDelete = findToDelete(localDir, allItemsAndLocalFiles);
		LOG.i("Identified %s items that might need deleting.", toDelete.size());
		storeResult(checkout, prgLstnr, toDelete);
	}

	private static List<? extends MlistItem> fetchListOfItems (final Checkout checkout, final ServerReference host) throws IOException {
		final MlistItemList itemList = MnApi.fetchDbItems(host, checkout.getDbRelativePath(), checkout.getQuery(), "audio_only", true); // TODO unhardcode this.
		return itemList.getMlistItemList();
	}

	private static List<ItemAndFile> computeLocalFiles (final File localDir, final List<? extends MlistItem> items, final List<String> srcs) throws UnsupportedEncodingException {
		final List<ItemAndFile> ret = new ArrayList<ItemAndFile>();
		for (final MlistItem item : items) {
			final String urlWithoutSrc = removeSrc(URLDecoder.decode(item.getRelativeUrl(), "UTF-8"), srcs);
			final File urlFile = new File(localDir, urlWithoutSrc);
			final File localFile = new File(urlFile.getParent(), item.getFileName());
			ret.add(new ItemAndFile(item, localFile));
		}
		return ret;
	}

	private static List<ItemAndFile> computeRequireDownloading (final List<ItemAndFile> items) {
		final List<ItemAndFile> ret = new ArrayList<ItemAndFile>();
		for (final ItemAndFile i : items) {
			// Comparing file size here does not work because repeat transcodes may produce different sized output,
			// yet they should be considered equivalent.
			if (!i.getLocalFile().exists()
					|| i.getLocalFile().lastModified() < i.getItem().getLastModified()) {
				ret.add(i);
			}
		}
		return ret;
	}

	private int requestTranscodes (final Checkout checkout, final int notificationId, final Builder notif, final ServerReference host, final List<ItemAndFile> toCopy) throws IOException {
		final List<MlistItem> toTranscode = new ArrayList<MlistItem>();
		for (final ItemAndFile i : toCopy) {
			if (i.getItem().getFileSize() < 1) {
				toTranscode.add(i.getItem());
			}
		}
		int transcodedCount = 0;
		for (final MlistItem i : toTranscode) {
			updateNotifProgress(notificationId, notif, String.format("transcoding %s of %s",
					transcodedCount, toTranscode.size()),
					toTranscode.size(), transcodedCount);
			MnApi.postToFile(host, checkout.getDbRelativePath(), i, "transcode");
			transcodedCount += 1;
		}
		return transcodedCount;
	}

	private SyncDlPrgListnr downloadFiles (final Checkout checkout, final int notificationId, final Builder notif, final ServerReference host, final List<ItemAndFile> toCopy) throws IOException {
		final SyncDlPrgListnr prgLstnr = new SyncDlPrgListnr(notificationId, notif, toCopy);
		for (final ItemAndFile i : toCopy) {
			final File dir = i.getLocalFile().getParentFile();
			if (!dir.exists() && !dir.mkdirs()) throw new IOException("Failed to create '" + dir.getAbsolutePath() + "'.");

			final File tempFile = File.createTempFile("_" + i.getLocalFile().getName(), ".part", i.getLocalFile().getParentFile());
			try {
				MnApi.downloadFile(host, checkout.getDbRelativePath(), i.getItem(), tempFile, prgLstnr);
				if (!tempFile.renameTo(i.getLocalFile())) {
					throw new IOException(String.format("Failed to move '%s' to '%s'.",
							tempFile.getAbsolutePath(), i.getLocalFile().getAbsolutePath()));
				}
				if (!i.getLocalFile().setLastModified(i.getItem().getLastModified())) {
					LOG.w("Failed to set last modified on file '%s.'", i.getLocalFile().getAbsolutePath());
				}
				LOG.i("Downloaded: %s", i.getLocalFile().getAbsolutePath());
				prgLstnr.itemComplete(i);
			}
			finally {
				if (tempFile.exists()) {
					LOG.w("Cleaning up abandoned temporary file: %s", tempFile.getAbsolutePath());
					tempFile.delete();
				}
			}
		}
		return prgLstnr;
	}

	private static List<File> findToDelete (final File localDir, final List<ItemAndFile> allItemsAndLocalFiles) throws IOException {
		final Set<File> localFiles = new HashSet<File>(allItemsAndLocalFiles.size());
		for (final ItemAndFile i : allItemsAndLocalFiles) {
			localFiles.add(i.getLocalFile());
		}

		final List<File> toDelete = new ArrayList<File>();
		FileHelper.recursiveList(localDir, new Listener<File>() {
			@Override
			public void onAnswer (final File file) {
				if (!localFiles.contains(file)) toDelete.add(file);
			}
		});
		return toDelete;
	}

	private void storeResult (final Checkout checkout, final SyncDlPrgListnr prgLstnr, final List<File> toDelete) {
		String status = String.format("Transfered %s items (%s).",
				prgLstnr.getTransferedItems(),
				FormaterHelper.readableFileSize(prgLstnr.getTransferedItemBytes()));
		if (toDelete.size() > 0) {
			status += String.format("  %s files to delete.", toDelete.size());
		}
		this.configDb.updateCheckout(this.configDb.getCheckout(checkout.getId()).withStatus(status));
	}

	protected static long totalTransferSize (final List<ItemAndFile> list) {
		long total = 0;
		for (final ItemAndFile i : list) {
			total += i.getItem().getFileSize();
		}
		return total;
	}

	private static String removeSrc (final String path, final List<String> srcs) {
		String matchedSrc = null;
		for (final String src : srcs) {
			if (path.startsWith(src)) {
				if (matchedSrc == null || matchedSrc.length() < src.length()) {
					matchedSrc = src;
				}
			}
		}
		if (matchedSrc == null) throw new IllegalStateException("Path does not match any srcs: " + path);
		return path.substring(matchedSrc.length() + 1);
	}

	private final class SyncDlPrgListnr implements DownloadProgressListener {

		private final long UPDATE_INTIVAL_NANOS = TimeUnit.SECONDS.toNanos(1);

		private final int notificationId;
		private final Builder notif;
		private final List<ItemAndFile> toCopy;
		private final long totalTransferSize;

		private long transferedItems = 0;
		private long transferedItemBytes = 0;
		private long updatedNanos = System.nanoTime();

		public SyncDlPrgListnr (final int notificationId, final Builder notif, final List<ItemAndFile> toCopy) {
			this.notificationId = notificationId;
			this.notif = notif;
			this.toCopy = toCopy;
			this.totalTransferSize = totalTransferSize(toCopy);
		}

		public void itemComplete(final ItemAndFile i) {
			this.transferedItems += 1;
			this.transferedItemBytes += i.getItem().getFileSize();
		}

		public long getTransferedItems () {
			return this.transferedItems;
		}

		public long getTransferedItemBytes () {
			return this.transferedItemBytes;
		}

		@Override
		public void downloadProgress (final int bytesRead, final int totalBytes) {
			if (System.nanoTime() - this.updatedNanos > this.UPDATE_INTIVAL_NANOS) {
				this.updatedNanos = System.nanoTime();
				final long transferedBytes = this.transferedItemBytes + bytesRead;
				updateNotifProgress(this.notificationId, this.notif, String.format("copied %s of %s (%s of %s)",
						this.transferedItems, this.toCopy.size(),
						FormaterHelper.readableFileSize(transferedBytes),
						FormaterHelper.readableFileSize(this.totalTransferSize)),
						10000, (int) ((transferedBytes / (float) this.totalTransferSize) * 10000f));
			}
		}

		@Override
		public boolean abortListener () {
			return false;
		}
	}

}
