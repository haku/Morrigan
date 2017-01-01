package com.vaguehope.morrigan.android;

import java.io.File;
import java.io.IOException;
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
import android.util.Log;

import com.vaguehope.morrigan.android.helper.DialogHelper.Listener;
import com.vaguehope.morrigan.android.helper.ExceptionHelper;
import com.vaguehope.morrigan.android.helper.FileHelper;
import com.vaguehope.morrigan.android.helper.FormaterHelper;
import com.vaguehope.morrigan.android.helper.HttpFileDownloadHandler.DownloadProgressListener;
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.MlistItemList;
import com.vaguehope.morrigan.android.model.MlistState;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.state.Checkout;
import com.vaguehope.morrigan.android.state.ConfigDb;

public class SyncCheckoutsService extends AwakeService {

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
			doSyncs(notificationId, notif);
			result = "Finished.";
		}
		catch (final Exception e) {
			Log.e(C.LOGTAG, "Sync failed.", e);
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

	private void doSyncs (final int notificationId, final Builder notif) throws IOException {
		final List<Checkout> checkouts = this.configDb.getCheckouts();
		for (final Checkout checkout : checkouts) {
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
	}

	private void syncCheckout (final Checkout checkout, final int notificationId, final Builder notif) throws IOException {
		updateNotifTitle(notificationId, notif, checkout.getQuery());

		final File localDir = new File(checkout.getLocalDir());
		if (!localDir.exists() && !localDir.mkdirs()) throw new IOException("Failed to create '" + localDir.getAbsolutePath() + "'.");

		final ServerReference host = this.configDb.getServer(checkout.getHostId());
		final MlistItemList itemList = MnApi.fetchDbItems(host, checkout.getDbRelativePath(), checkout.getQuery(), "audio_only"); // TODO unhardcode this.
		final List<? extends MlistItem> items = itemList.getMlistItemList();

		final long totalSize = totalSize(items);
		updateNotifProgress(notificationId, notif, String.format("Checking %s items (%s) ...",
				items.size(), FormaterHelper.readableFileSize(totalSize)));

		final MlistState dbSrcs = MnApi.fetchDbSrcs(host, checkout.getDbRelativePath());
		final List<String> srcs = dbSrcs.getSrcs();

		final List<ToCopy> toCopy = new ArrayList<ToCopy>();
		final Set<File> allFiles = new HashSet<File>(items.size());
		for (final MlistItem item : items) {
			final File localFile = new File(localDir, removeSrc(URLDecoder.decode(item.getRelativeUrl(), "UTF-8"), srcs));
			if (!localFile.exists() || localFile.lastModified() < item.getLastModified()) {
				toCopy.add(new ToCopy(item, localFile));
			}
			allFiles.add(localFile);
		}

		final long spaceAfterCopy = localDir.getFreeSpace() - totalTransferSize(toCopy);
		if (spaceAfterCopy < 1) throw new IOException(String.format(
				"Not enough space on device: %s short.",
				FormaterHelper.readableFileSize(Math.abs(spaceAfterCopy))));

		final SyncDlPrgListnr prgLstnr = new SyncDlPrgListnr(notificationId, notif, toCopy);
		for (final ToCopy i : toCopy) {
			final File dir = i.getLocalFile().getParentFile();
			if (!dir.exists() && !dir.mkdirs()) throw new IOException("Failed to create '" + dir.getAbsolutePath() + "'.");

			MnApi.downloadFile(host, checkout.getDbRelativePath(), i.getItem(), i.getLocalFile(), prgLstnr);
			i.getLocalFile().setLastModified(i.getItem().getLastModified());
			prgLstnr.itemComplete(i);
		}

		final List<File> toDelete = new ArrayList<File>();
		FileHelper.recursiveList(localDir, new Listener<File>() {
			@Override
			public void onAnswer (final File file) {
				if (!allFiles.contains(file)) toDelete.add(file);
			}
		});

		String status = String.format("Transfered %s items (%s).",
				prgLstnr.getTransferedItems(),
				FormaterHelper.readableFileSize(prgLstnr.getTransferedItemBytes()));
		if (toDelete.size() > 0) status += String.format("  %s files to delete.", toDelete.size());
		this.configDb.updateCheckout(this.configDb.getCheckout(checkout.getId()).withStatus(status));
	}

	private static long totalSize (final List<? extends MlistItem> items) {
		long total = 0;
		for (final MlistItem i : items) {
			total += i.getFileSize();
		}
		return total;
	}

	protected static long totalTransferSize (final List<ToCopy> list) {
		long total = 0;
		for (final ToCopy i : list) {
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

	private static class ToCopy {

		private final MlistItem item;
		private final File localFile;

		public ToCopy (final MlistItem item, final File localFile) {
			this.item = item;
			this.localFile = localFile;
		}

		public MlistItem getItem () {
			return this.item;
		}

		public File getLocalFile () {
			return this.localFile;
		}
	}

	private final class SyncDlPrgListnr implements DownloadProgressListener {

		private final long UPDATE_INTIVAL_NANOS = TimeUnit.SECONDS.toNanos(1);

		private final int notificationId;
		private final Builder notif;
		private final List<ToCopy> toCopy;
		private final long totalTransferSize;

		private long transferedItems = 0;
		private long transferedItemBytes = 0;
		private final long currentFileSize = 0;
		private long updatedNanos = System.nanoTime();

		public SyncDlPrgListnr (final int notificationId, final Builder notif, final List<ToCopy> toCopy) {
			this.notificationId = notificationId;
			this.notif = notif;
			this.toCopy = toCopy;
			this.totalTransferSize = totalTransferSize(toCopy);
		}

		public void itemComplete(final ToCopy i) {
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
