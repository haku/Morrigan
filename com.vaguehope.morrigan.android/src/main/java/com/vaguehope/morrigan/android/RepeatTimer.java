package com.vaguehope.morrigan.android;

import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;

public abstract class RepeatTimer {

	private final Handler handler = new Handler();
	private final int intervalSeconds;
	private final AtomicBoolean enabled = new AtomicBoolean(false);

	public RepeatTimer (int intervalSeconds) {
		this.intervalSeconds = intervalSeconds;
	}

	public abstract void timer ();

	public void start () {
		this.enabled.set(true);
		scheduleNow();
	}

	public void stop () {
		this.enabled.set(false);
		this.handler.removeCallbacks(this.runnable);
	}

	public boolean isEnabled () {
		return this.enabled.get();
	}

	protected void scheduleNow () {
		this.handler.post(this.runnable);
	}

	protected void scheduleAfterDelay () {
		this.handler.postDelayed(this.runnable, this.intervalSeconds * 1000L);
	}

	private final Runnable runnable = new Runnable() {
		@Override
		public void run () {
			if (!isEnabled()) return;
			try {
				timer();
			}
			finally {
				if (isEnabled()) scheduleAfterDelay();
			}
		}
	};

}
