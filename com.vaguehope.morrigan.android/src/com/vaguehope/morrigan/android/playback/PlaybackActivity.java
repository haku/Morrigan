package com.vaguehope.morrigan.android.playback;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.vaguehope.morrigan.android.ErrorsList;
import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.ServerActivity;
import com.vaguehope.morrigan.android.checkout.CheckoutMgrActivity;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.playback.Playbacker.PlaybackWatcher;

public class PlaybackActivity extends Activity {

	private static final LogWrapper LOG = new LogWrapper("PA");

	private MessageHandler messageHandler;
	private ErrorsList errorsList;
	private QueueAdapter queueAdaptor;
	private TextView txtTitle;
	private TextView txtQueue;
	private ImageView imgPlaystate;

	// Activity life-cycle.

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.messageHandler = new MessageHandler(this);
		wireGui();
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

	protected static LogWrapper getLog () {
		return LOG;
	}

	// Playback service.

	private PlaybackClient bndPb;

	private void resumeDb () {
		if (this.bndPb == null) {
			LOG.d("Binding playback service...");
			this.bndPb = new PlaybackClient(this, LOG.getPrefix(), new Runnable() {
				@Override
				public void run () {
					/*
					 * this convoluted method is because the service connection
					 * won't finish until this thread processes messages again
					 * (i.e., after it exits this thread). if we try to talk to
					 * the DB service before then, it will NPE.
					 */
					getPlaybacker().addPlaybackListener(getPlaybackWatcher());
					getLog().d("Playback service bound.");
				}
			});
		}
		else { // because we stop listening in onPause(), we must resume if the user comes back.
			this.bndPb.getService().addPlaybackListener(getPlaybackWatcher());
			LOG.d("Playback service rebound.");
		}
	}

	private void suspendDb () {
		// We might be pausing before the callback has come.
		final Playbacker pb = this.bndPb.getService();
		if (pb != null) {
			pb.removePlaybackListener(getPlaybackWatcher());
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

	public Playbacker getPlaybacker () {
		final PlaybackClient d = this.bndPb;
		if (d == null) return null;
		return d.getService();
	}

	// GUI wiring.

	private void wireGui () {
		setContentView(R.layout.playbackactivity);

		final ActionBar ab = getActionBar();
		ab.setDisplayShowHomeEnabled(true);
		ab.setHomeButtonEnabled(true);

		final ListView lstErrors = (ListView) findViewById(R.id.lstErrors);
		this.errorsList = new ErrorsList(this, lstErrors);

		this.queueAdaptor = new QueueAdapter(this);
		final ListView lstQueue = (ListView) findViewById(R.id.lstQueue);
		lstQueue.setAdapter(this.queueAdaptor);
//		lstQueue.setOnItemClickListener(...);
		lstQueue.setOnCreateContextMenuListener(this.queueContextMenuListener);

		final View tagRow = findViewById(R.id.tagRow);
//		tagRow.setOnClickListener(this.contextMenuClickListener);
//		tagRow.setOnCreateContextMenuListener(this.tagRowContextMenuListener);

		this.txtTitle = (TextView) findViewById(R.id.txtTitle);
		this.txtQueue = (TextView) findViewById(R.id.txtQueue);
		this.imgPlaystate = (ImageView) findViewById(R.id.imgPlaystate);

		findViewById(R.id.btnSearch).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				search();
			}
		});
		findViewById(R.id.btnPlaypause).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				playpause();
			}
		});
		findViewById(R.id.btnPlaypause).setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick (final View v) {
				stop();
				return true;
			}
		});
		findViewById(R.id.btnNext).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				next();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu (final Menu menu) {
		getMenuInflater().inflate(R.menu.playbackmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// TODO use this?
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

	protected PlaybackWatcher getPlaybackWatcher () {
		return this.playbackWatcher;
	}

	private final PlaybackWatcher playbackWatcher = new PlaybackWatcher() {
		@Override
		public void queueChanged () {
			PlaybackActivity.this.messageHandler.sendEmptyMessage(Msgs.QUEUE_CHANGED.ordinal());
		}

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
	};

	protected enum Msgs {
		QUEUE_CHANGED,
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
		final Object obj = msg.obj;
		msg.recycle();
		switch (m) {
			case QUEUE_CHANGED:
				refreshUi();
				break;
			case PLAYBACK_LOADING:
				setIcon(R.drawable.next);
				final QueueItem item = (QueueItem) obj;
				this.txtTitle.setText(item.getTitle());
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
		PlaybackActivity.this.imgPlaystate.setImageResource(resId);
	}

	private void refreshUi () {
		final Playbacker pb = getPlaybacker();
		if (pb != null) {
			final List<QueueItem> items = pb.getQueue();
			this.queueAdaptor.setInputData(items);
			this.txtQueue.setText(items.size() + " items.");
		}
		else {
			LOG.w("Failed to refresh queue as playback service was not bound.");
		}
	}

	// Queue.

	private static final int MENU_CTX_REMOVE = 1;

	private final OnCreateContextMenuListener queueContextMenuListener = new OnCreateContextMenuListener() {
		@Override
		public void onCreateContextMenu (final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			final QueueItem item = PlaybackActivity.this.queueAdaptor.getQueueItem(info.position);
			menu.setHeaderTitle(item.getTitle());
			menu.add(Menu.NONE, MENU_CTX_REMOVE, Menu.NONE, "Remove");
		}
	};

	@Override
	public boolean onContextItemSelected (final MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			case MENU_CTX_REMOVE:
				final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
				final Playbacker pb = getPlaybacker();
				if (pb != null) {
					pb.getQueue().remove(info.position);
					pb.notifyQueueChanged();
				}
				return true;
			default:
				return super.onContextItemSelected(menuItem);
		}
	}

	// Buttons.

	private static final int REQUEST_CODE = 10; // TODO do this better.

	protected void search () {
		final Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, REQUEST_CODE);
	}

	protected void playpause () {
		final Playbacker pb = getPlaybacker();
		if (pb != null) {
			pb.playPausePlayback();
		}
	}

	protected void stop () {
		final Playbacker pb = getPlaybacker();
		if (pb != null) {
			pb.stopPlayback();
		}
	}

	protected void next () {
		final Playbacker pb = getPlaybacker();
		if (pb != null) {
			pb.gotoNextItem();
		}
	}

	@Override
	protected void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK && requestCode == 10) {
			final Uri mediaUri = data.getData();
			final Playbacker pb = getPlaybacker();
			if (pb != null) {
				QueueItem item = new QueueItem(this, mediaUri);
				pb.getQueue().add(item);
				pb.notifyQueueChanged();
				LOG.i("Added to queue: %s", item);
			}
		}
	}

}
