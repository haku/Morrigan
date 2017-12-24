package com.vaguehope.morrigan.android.playback;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.LogWrapper;

public class MnPreferenceActivity extends PreferenceActivity {

	private static final LogWrapper LOG = new LogWrapper("PA");

	@Override
	protected void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onBuildHeaders (final List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	public void onResume () {
		super.onResume();
		resumeDb();
	}

	@Override
	public void onPause () {
		suspendDb();
		super.onPause();
	}

	@Override
	protected void onDestroy () {
		disposeDb();
		super.onDestroy();
	}

	// Playback service.

	private MediaClient bndPb;
	private final Set<Runnable> onDbBoundListeners = new CopyOnWriteArraySet<Runnable>();

	private void resumeDb () {
		if (this.bndPb == null) {
			LOG.d("Binding playback service...");
			this.bndPb = new MediaClient(this, LOG.getPrefix(), new Runnable() {
				@Override
				public void run () {
					/*
					 * this convoluted method is because the service connection
					 * won't finish until this thread processes messages again
					 * (i.e., after it exits this thread). if we try to talk to
					 * the DB service before then, it will NPE.
					 */
					LOG.d("Media service bound.");
					runOnDbBoundListeners();
				}
			});
		}
		else { // because we stop listening in onPause(), we must resume if the user comes back.
			LOG.d("Media service rebound.");
			runOnDbBoundListeners();
		}
	}

	private void suspendDb () {
		// We might be pausing before the callback has come.
		final MediaServices pb = this.bndPb.getService();
		if (pb != null) {
			//TODO pb.getPlaybacker().removePlaybackListener(this.playbackWatcher);
		}
		else {
			// If we have not even had the callback yet, cancel it.
			this.bndPb.clearReadyListener();
		}
		LOG.d("Media service released.");
	}

	private void disposeDb () {
		if (this.bndPb != null) this.bndPb.dispose();
	}

	protected void addOnMediaDbBound(final Runnable listener) {
		this.onDbBoundListeners.add(listener);

		// If already bound, run listener now.
		final MediaClient mc = this.bndPb;
		if (mc != null) {
			final MediaServices ms = mc.getService();
			if (ms != null) {
				listener.run();
			}
		}
	}

	protected void removeOnMediaDbBound(final Runnable listener) {
		this.onDbBoundListeners.remove(listener);
	}

	private void runOnDbBoundListeners () {
		for (final Runnable r : MnPreferenceActivity.this.onDbBoundListeners) {
			r.run();
		}
	}

	protected MediaDb getMediaDb () {
		final MediaClient d = this.bndPb;
		if (d == null) throw new IllegalStateException("Service not bound.");
		return d.getService().getMediaDb();
	}

	// Fragment safety.

	@Override
	protected boolean isValidFragment (final String fragmentName) {
		if (fragmentName == null) return false;
		return fragmentName.startsWith(this.getClass().getPackage().getName());
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
