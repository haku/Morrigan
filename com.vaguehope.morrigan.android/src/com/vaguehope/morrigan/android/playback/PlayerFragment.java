package com.vaguehope.morrigan.android.playback;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.playback.Playbacker.PlaybackWatcher;

public class PlayerFragment extends Fragment {

	private static final LogWrapper LOG = new LogWrapper("PF");

	private MessageHandler messageHandler;
	private PlaybackActivity hostActivity;

	private QueueAdapter queueAdaptor;
	private TextView txtTitle;
	private TextView txtQueue;

	@Override
	public View onCreateView (final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		this.hostActivity = (PlaybackActivity) getActivity();
		this.messageHandler = new MessageHandler(this);

		final View rootView = inflater.inflate(R.layout.playback_player, container, false);
		wireGui(rootView);
		return rootView;
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
	public void onDestroy () {
		disposeDb();
		super.onDestroy();
	}

	// Playback service.

	private MediaClient bndPb;

	private void resumeDb () {
		if (this.bndPb == null) {
			LOG.d("Binding playback service...");
			this.bndPb = new MediaClient(getActivity(), LOG.getPrefix(), new Runnable() {
				@Override
				public void run () {
					/*
					 * this convoluted method is because the service connection
					 * won't finish until this thread processes messages again
					 * (i.e., after it exits this thread). if we try to talk to
					 * the DB service before then, it will NPE.
					 */
					getPlaybacker().addPlaybackListener(PlayerFragment.this.playbackWatcher);
					LOG.d("Playback service bound.");
				}
			});
		}
		else if (getPlaybacker() != null) { // because we stop listening in onPause(), we must resume if the user comes back.
			getPlaybacker().addPlaybackListener(this.playbackWatcher);
			LOG.d("Playback service rebound.");
		}
		else {
			LOG.w("resumePb() called while playback service is half bound.  I do not know what to do.");
		}
	}

	private void suspendDb () {
		// We might be pausing before the callback has come.
		final MediaServices pb = this.bndPb.getService();
		if (pb != null) { // We might be pausing before the callback has come.
			pb.getPlaybacker().removePlaybackListener(this.playbackWatcher);
		}
		else { // If we have not even had the callback yet, cancel it.
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

	// Fragment callbacks.

	@Override
	public void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
		LOG.i("onActivityResult: requestCode=%s resultCode=%s", requestCode, resultCode);
		if (resultCode == Activity.RESULT_OK && requestCode == PlaybackCodes.BROWSE_MEDIA_REQUEST_CODE) {
			addUriToQueue(data.getData());
		}
	}

	@Override
	public boolean onContextItemSelected (final MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			case PlaybackCodes.MENU_REMOVE:
				final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
				removeFromQueueByPosition(info.position);
				return true;
			default:
				return super.onContextItemSelected(menuItem);
		}
	}

	// GUI.

	private void wireGui (final View rootView) {
		this.queueAdaptor = new QueueAdapter(this.hostActivity);
		final ListView lstQueue = (ListView) rootView.findViewById(R.id.lstQueue);
		lstQueue.setAdapter(this.queueAdaptor);
//		lstQueue.setOnItemClickListener(...);
		lstQueue.setOnCreateContextMenuListener(this.queueContextMenuListener);

		final View tagRow = rootView.findViewById(R.id.tagRow);
//		tagRow.setOnClickListener(this.contextMenuClickListener);
//		tagRow.setOnCreateContextMenuListener(this.tagRowContextMenuListener);

		this.txtTitle = (TextView) rootView.findViewById(R.id.txtTitle);
		this.txtQueue = (TextView) rootView.findViewById(R.id.txtQueue);

		rootView.findViewById(R.id.btnSearch).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				search();
			}
		});
		rootView.findViewById(R.id.btnPlaypause).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				playpause();
			}
		});
		rootView.findViewById(R.id.btnPlaypause).setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick (final View v) {
				stop();
				return true;
			}
		});
		rootView.findViewById(R.id.btnNext).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				next();
			}
		});
	}

	protected PlaybackWatcher getPlaybackWatcher () {
		return this.playbackWatcher;
	}

	private final PlaybackWatcher playbackWatcher = new PlaybackWatcherAdapter() {
		@Override
		public void queueChanged () {
			PlayerFragment.this.messageHandler.sendEmptyMessage(Msgs.QUEUE_CHANGED.ordinal());
		}

		@Override
		public void playbackLoading (final QueueItem item) {
			final Message msg = PlayerFragment.this.messageHandler.obtainMessage(Msgs.PLAYBACK_LOADING.ordinal());
			msg.obj = item;
			msg.sendToTarget();
		}
	};

	protected enum Msgs {
		QUEUE_CHANGED,
		PLAYBACK_LOADING;
		public static final Msgs values[] = values(); // Optimisation to avoid new array every time.
	}

	private static class MessageHandler extends Handler {

		private final WeakReference<PlayerFragment> parentRef;

		public MessageHandler (final PlayerFragment playerFragment) {
			this.parentRef = new WeakReference<PlayerFragment>(playerFragment);
		}

		@Override
		public void handleMessage (final Message msg) {
			final PlayerFragment parent = this.parentRef.get();
			if (parent != null) parent.msgOnUiThread(msg);
		}
	}

	protected void msgOnUiThread (final Message msg) {
		final Msgs m = Msgs.values[msg.what];
		switch (m) {
			case QUEUE_CHANGED:
				refreshQueue();
				break;
			case PLAYBACK_LOADING:
				final QueueItem item = (QueueItem) msg.obj;
				this.txtTitle.setText(item.getTitle());
				break;
			default:
		}
	}

	private void refreshQueue () {
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

	private void addUriToQueue (final Uri mediaUri) {
		final Playbacker pb = getPlaybacker();
		if (pb != null) {
			QueueItem item = new QueueItem(getActivity(), mediaUri);
			pb.getQueue().add(item);
			pb.notifyQueueChanged();
			LOG.i("Added to queue: %s", item);
		}
	}

	private final OnCreateContextMenuListener queueContextMenuListener = new OnCreateContextMenuListener() {
		@Override
		public void onCreateContextMenu (final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			final QueueItem item = PlayerFragment.this.queueAdaptor.getQueueItem(info.position);
			menu.setHeaderTitle(item.getTitle());
			menu.add(Menu.NONE, PlaybackCodes.MENU_REMOVE, Menu.NONE, "Remove");
		}
	};

	private void removeFromQueueByPosition (final int position) {
		final Playbacker pb = getPlaybacker();
		if (pb != null) {
			pb.getQueue().remove(position);
			pb.notifyQueueChanged();
		}
	}

	// Buttons.

	protected void search () {
		final Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, PlaybackCodes.BROWSE_MEDIA_REQUEST_CODE);
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

}
