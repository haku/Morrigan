package com.vaguehope.morrigan.android;

import java.util.List;

import com.vaguehope.morrigan.android.helper.ExceptionHelper;
import com.vaguehope.morrigan.android.state.Checkout;
import com.vaguehope.morrigan.android.state.ConfigDb;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SyncCheckoutsService extends AwakeService {

	private final int notificationId;
	private ConfigDb configDb;
	private NotificationManager notifMgr;

	public SyncCheckoutsService () {
		super("SyncCheckoutsService");
		this.notificationId = (int) (System.currentTimeMillis()); // Probably unique.
	}

	@Override
	public void onCreate () {
		super.onCreate();
		this.configDb = new ConfigDb(this);
	}

	@Override
	protected void doWork (final Intent i) {
		this.notifMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		final Builder notif = makeNotif("Sync starting...");
		updateNotif(notif);
		String result = "Unknown result.";
		try {
			doSyncs(notif);
			result = "Finished.";
		}
		catch (final Exception e) {
			Log.e(C.LOGTAG, "Sync failed.", e);
			result = ExceptionHelper.veryShortMessage(e);
		}
		finally {
			updateNotifResult(notif, result);
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

	private void updateNotif (final Builder notif, final String msg) {
		updateNotif(notif.setContentText(msg));
	}

	private void updateNotifProgress (final Builder notif, final int max, final int progress) {
		updateNotif(notif.setProgress(max, progress, false));
	}

	private void updateNotifResult (final Builder notif, final String msg) {
		updateNotif(notif.setOngoing(false)
				.setProgress(0, 0, false)
				.setContentText(msg));
	}

	private void updateNotif (final Builder notif) {
		this.notifMgr.notify(this.notificationId, notif.getNotification());
	}

	private void doSyncs (final Builder notif) {
		final List<Checkout> checkouts = this.configDb.getCheckouts();
		for (final Checkout checkout : checkouts) {
			syncCheckout(checkout, notif);
		}
	}

	private void syncCheckout (final Checkout checkout, final Builder notif) {
		updateNotif(notif, checkout.getQuery());
		for (int i = 0; i < 10; i++) {
			updateNotifProgress(notif, 10, i);
			try {
				Thread.sleep(1000L);
			}
			catch (final InterruptedException e) {}
		}
	}

}
