package com.vaguehope.morrigan.android;

import java.io.IOException;
import java.util.List;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.vaguehope.morrigan.android.helper.ExceptionHelper;
import com.vaguehope.morrigan.android.helper.FormaterHelper;
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.MlistItemList;
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

		final Builder notif = makeNotif("Sync starting...");
		updateNotif(notificationId, notif);
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

	private Builder makeNotif (final CharSequence msg) {
		return new Notification.Builder(this)
				.setSmallIcon(R.drawable.pref)
				.setContentTitle("Syncing Media")
				.setContentText(msg)
				.setTicker(msg)
				.setOngoing(true);
	}

	private void updateNotif (final int notificationId, final Builder notif, final String msg) {
		updateNotif(notificationId, notif.setContentText(msg));
	}

	private void updateNotifProgress (final int notificationId, final Builder notif, final int max, final int progress) {
		updateNotif(notificationId, notif.setProgress(max, progress, false));
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
			syncCheckout(checkout, notificationId, notif);
		}
	}

	private void syncCheckout (final Checkout checkout, final int notificationId, final Builder notif) throws IOException {
		updateNotif(notificationId, notif, checkout.getQuery());

		final MlistItemList itemList = MnApi.fetchDbItems(this.configDb.getServer(checkout.getHostId()),
				checkout.getDbRelativePath(),
				checkout.getQuery());
		final List<? extends MlistItem> items = itemList.getMlistItemList();

		final long totalSize = totalSize(items);
		updateNotif(notificationId, notif, String.format("%s ... %s items (%s)",
				checkout.getQuery(), items.size(), FormaterHelper.readableFileSize(totalSize)));

		for (int i = 0; i < 10; i++) {
			updateNotifProgress(notificationId, notif, 10, i);
			try {
				Thread.sleep(1000L);
			}
			catch (final InterruptedException e) {}
		}

		// TODO stash result in DB.
	}

	private static long totalSize (final List<? extends MlistItem> items) {
		long total = 0;
		for (final MlistItem i : items) {
			total += i.getFileSize();
		}
		return total;
	}

}
