package com.vaguehope.morrigan.android.playback;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;

import com.vaguehope.morrigan.android.helper.LogWrapper;

public class PlaybackBroadcastReceiver extends BroadcastReceiver {

	private static final String EXTRA_ACTION_CODE = "action_code";

	private static final LogWrapper LOG = new LogWrapper("PAR");

	private final MediaService playbackService;

	public PlaybackBroadcastReceiver (final MediaService playbackService) {
		this.playbackService = playbackService;
	}

	public static PendingIntent makePendingIntent (final Context context, final int actionCode) {
		if (actionCode < 1) throw new IllegalArgumentException("actionCode must be positive.");
		return PendingIntent.getBroadcast(context, actionCode,
				new Intent(PlaybackCodes.ACTION_PLAYBACK)
						.putExtra(EXTRA_ACTION_CODE, actionCode),
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	@Override
	public void onReceive (final Context context, final Intent intent) {
		if (PlaybackCodes.ACTION_PLAYBACK.equals(intent.getAction())) {
			final Bundle extras = intent.getExtras();
			if (extras != null) {
				final int actionCode = extras.getInt(EXTRA_ACTION_CODE);
				LOG.i("actionCode=%s", actionCode);
				this.playbackService.onBroadcastAction(actionCode);
			}
		}
		else if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
			LOG.i("Handling ACTION_AUDIO_BECOMING_NOISY...");
			this.playbackService.onBroadcastAction(PlaybackCodes.ACTION_PAUSE);
		}
		else {
			LOG.w("Unexpected intent action: %s", intent.getAction());
		}
	}

}
