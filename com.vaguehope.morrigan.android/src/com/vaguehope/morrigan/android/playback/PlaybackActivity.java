package com.vaguehope.morrigan.android.playback;

import java.lang.ref.WeakReference;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ColumnTitleStrip;
import android.support.v4.view.ColumnTitleStrip.ColumnClickListener;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.ViewGroup.LayoutParams;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.ServerActivity;
import com.vaguehope.morrigan.android.checkout.CheckoutMgrActivity;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.playback.Playbacker.PlayOrder;
import com.vaguehope.morrigan.android.playback.Playbacker.PlaybackWatcher;

public class PlaybackActivity extends FragmentActivity {

	private static final LogWrapper LOG = new LogWrapper("PA");

	private MessageHandler messageHandler;
	private ViewPager viewPager;
	private SectionsPagerAdapter sectionsPagerAdapter;
	private ColumnTitleStrip columnTitleStrip;

	// Activity life-cycle.

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.messageHandler = new MessageHandler(this);
		setContentView(R.layout.playback_activity);

		this.sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		this.viewPager = (ViewPager) findViewById(R.id.viewpager);
		this.viewPager.setAdapter(this.sectionsPagerAdapter);

		final ActionBar ab = getActionBar();
		ab.setDisplayShowHomeEnabled(true);
		ab.setHomeButtonEnabled(true);
		ab.setDisplayShowTitleEnabled(false);
		ab.setDisplayShowCustomEnabled(true);

		this.columnTitleStrip = new ColumnTitleStrip(ab.getThemedContext());
		this.columnTitleStrip.setViewPager(this.viewPager);
		this.columnTitleStrip.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		this.columnTitleStrip.setColumnClickListener(new ColumnClickListener() {
			@Override
			public void onColumnTitleClick (final int position) {
				PlaybackActivity.this.viewPager.setCurrentItem(position);
			}
		});
		ab.setCustomView(this.columnTitleStrip);
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
					getPlaybacker().addPlaybackListener(PlaybackActivity.this.playbackWatcher);
					LOG.d("Playback service bound.");
				}
			});
		}
		else { // because we stop listening in onPause(), we must resume if the user comes back.
			this.bndPb.getService().getPlaybacker().addPlaybackListener(this.playbackWatcher);
			LOG.d("Playback service rebound.");
		}
	}

	private void suspendDb () {
		// We might be pausing before the callback has come.
		final MediaServices pb = this.bndPb.getService();
		if (pb != null) {
			pb.getPlaybacker().removePlaybackListener(this.playbackWatcher);
		}
		else {
			// If we have not even had the callback yet, cancel it.
			this.bndPb.clearReadyListener();
		}
		LOG.d("Playback service released.");
	}

	private void disposeDb () {
		if (this.bndPb != null) this.bndPb.dispose();
	}

	protected Playbacker getPlaybacker () {
		final MediaClient d = this.bndPb;
		if (d == null) return null;
		return d.getService().getPlaybacker();
	}

	protected MediaDb getMediaDb () {
		final MediaClient d = this.bndPb;
		if (d == null) return null;
		return d.getService().getMediaDb();
	}

	protected SectionsPagerAdapter getSectionsPagerAdapter() {
		return this.sectionsPagerAdapter;
	}

	// GUI wiring.

	@Override
	public boolean onCreateOptionsMenu (final Menu menu) {
		getMenuInflater().inflate(R.menu.playbackmenu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu (final Menu menu) {
		final int id;
		switch (getPlaybacker().getPlayOrder()) {
			case QUEUE_ONLY:
				id = R.id.playorder_queue_only;
				break;
			case RANDOM:
				id = R.id.playorder_random;
				break;
			default:
				throw new IllegalStateException("Unknown playback order.");
		}

		final SubMenu playorderMenu = menu.findItem(R.id.playorder).getSubMenu();
		for (int i = 0; i < playorderMenu.size(); i++) {
			final MenuItem item = playorderMenu.getItem(i);
			item.setChecked(item.getItemId() == id);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// TODO use this?
				return true;
			case R.id.playorder_queue_only:
				getPlaybacker().setPlayOrder(PlayOrder.QUEUE_ONLY);
				return true;
			case R.id.playorder_random:
				getPlaybacker().setPlayOrder(PlayOrder.RANDOM);
				return true;
			case R.id.queue_shuffle:
				getMediaDb().shuffleQueue();
				return true;
			case R.id.queue_clear:
				getMediaDb().clearQueue();
				return true;
			case R.id.preferences:
				startActivity(new Intent(this, MnPreferenceActivity.class));
				return true;
			case R.id.remotecontrol:
				startActivity(new Intent(getApplicationContext(), ServerActivity.class)
						.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
				return true;
			case R.id.checkoutmgr:
				startActivity(new Intent(getApplicationContext(), CheckoutMgrActivity.class)
						.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private final PlaybackWatcher playbackWatcher = new PlaybackWatcherAdapter() {
		@Override
		public void playbackLoading (final QueueItem item) {
			final Message msg = PlaybackActivity.this.messageHandler.obtainMessage(Msgs.PLAYBACK_LOADING.ordinal());
			msg.obj = item;
			msg.sendToTarget();
		}

		@Override
		public void playbackPlaying () {
			PlaybackActivity.this.messageHandler.sendEmptyMessage(Msgs.PLAYBACK_PLAYING.ordinal());
		}

		@Override
		public void playbackPaused () {
			PlaybackActivity.this.messageHandler.sendEmptyMessage(Msgs.PLAYBACK_PAUSED.ordinal());
		}

		@Override
		public void playbackStopped () {
			PlaybackActivity.this.messageHandler.sendEmptyMessage(Msgs.PLAYBACK_STOPPED.ordinal());
		}

		@Override
		public void playbackError () {
			PlaybackActivity.this.messageHandler.sendEmptyMessage(Msgs.PLAYBACK_ERROR.ordinal());
		}

		@Override
		public void exitRequested () {
			finish();
		}
	};

	protected enum Msgs {
		PLAYBACK_LOADING,
		PLAYBACK_PLAYING,
		PLAYBACK_PAUSED,
		PLAYBACK_STOPPED,
		PLAYBACK_ERROR;
		public static final Msgs values[] = values(); // Optimisation to avoid new array every time.
	}

	private static class MessageHandler extends Handler {

		private final WeakReference<PlaybackActivity> parentRef;

		public MessageHandler (final PlaybackActivity parent) {
			this.parentRef = new WeakReference<PlaybackActivity>(parent);
		}

		@Override
		public void handleMessage (final Message msg) {
			final PlaybackActivity parent = this.parentRef.get();
			if (parent != null) parent.msgOnUiThread(msg);
		}
	}

	protected void msgOnUiThread (final Message msg) {
		final Msgs m = Msgs.values[msg.what];
		switch (m) {
			case PLAYBACK_LOADING:
				setIcon(R.drawable.next);
				break;
			case PLAYBACK_PLAYING:
				setIcon(R.drawable.play);
				break;
			case PLAYBACK_PAUSED:
				setIcon(R.drawable.pause);
				break;
			case PLAYBACK_STOPPED:
				setIcon(R.drawable.stop);
				break;
			case PLAYBACK_ERROR:
				setIcon(R.drawable.exclamation_red);
				break;
			default:
		}
	}

	private void setIcon (final int resId) {
		getActionBar().setIcon(resId);
	}

	@Override
	public void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
		LOG.i("onActivityResult: requestCode=%s resultCode=%s", requestCode, resultCode);
		super.onActivityResult(requestCode, resultCode, data);
	}

}
