package com.vaguehope.morrigan.android.playback;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.helper.Result;
import com.vaguehope.morrigan.android.playback.MediaDb.MediaWatcher;
import com.vaguehope.morrigan.android.playback.MediaDb.MoveAction;
import com.vaguehope.morrigan.android.playback.MediaDb.QueueEnd;
import com.vaguehope.morrigan.android.playback.Playbacker.PlaybackWatcher;

public class PlayerFragment extends Fragment {

	private static final LogWrapper LOG = new LogWrapper("PF");

	private MessageHandler messageHandler;

	private ListView queueList;

	private TextView txtTitle;
	private TextView txtTags;
	private TextView txtQueue;
	private View btnPlayPause;

	private QueueCursorAdapter adapter;
	private ScrollState scrollState;

	@Override
	public View onCreateView (final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		this.messageHandler = new MessageHandler(this);

		final View rootView = inflater.inflate(R.layout.playback_player, container, false);
		wireGui(rootView, container);
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

	private MediaClient bndMc;

	private void resumeDb () {
		if (this.bndMc == null) {
			LOG.d("Binding playback service...");
			this.bndMc = new MediaClient(getActivity(), LOG.getPrefix(), new Runnable() {
				@Override
				public void run () {
					/*
					 * this convoluted method is because the service connection
					 * won't finish until this thread processes messages again
					 * (i.e., after it exits this thread). if we try to talk to
					 * the DB service before then, it will NPE.
					 */
					getMediaDb().addMediaWatcher(PlayerFragment.this.mediaWatcher);
					getPlaybacker().addPlaybackListener(PlayerFragment.this.playbackWatcher);
					LOG.d("Playback service bound.");
				}
			});
		}
		else if (getPlaybacker() != null) { // because we stop listening in onPause(), we must resume if the user comes back.
			getMediaDb().addMediaWatcher(this.mediaWatcher);
			getPlaybacker().addPlaybackListener(this.playbackWatcher);
			LOG.d("Playback service rebound.");
		}
		else {
			LOG.w("resumePb() called while playback service is half bound.  I do not know what to do.");
		}
	}

	private void suspendDb () {
		// We might be pausing before the callback has come.
		final MediaServices ms = this.bndMc.getService();
		if (ms != null) { // We might be pausing before the callback has come.
			ms.getPlaybacker().removePlaybackListener(this.playbackWatcher);
			ms.getMediaDb().removeMediaWatcher(this.mediaWatcher);
		}
		else { // If we have not even had the callback yet, cancel it.
			this.bndMc.clearReadyListener();
		}
		LOG.d("Playback service released.");
	}

	private void disposeDb () {
		if (this.adapter != null) this.adapter.dispose();
		if (this.bndMc != null) this.bndMc.dispose();
	}

	protected Playbacker getPlaybacker () {
		final MediaClient d = this.bndMc;
		if (d == null) return null;
		return d.getService().getPlaybacker();
	}

	protected MediaDb getMediaDb () {
		final MediaClient d = this.bndMc;
		if (d == null) return null;
		return d.getService().getMediaDb();
	}

	protected QueueCursorAdapter getAdapter () {
		return this.adapter;
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
			case PlaybackCodes.MENU_MOVE_TOP:
				moveQueueItemTopById(((AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo()).id);
				return true;
			case PlaybackCodes.MENU_MOVE_BOTTOM:
				moveQueueItemBottomById(((AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo()).id);
				return true;
			case PlaybackCodes.MENU_MOVE_UP:
				moveQueueItemUpById(((AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo()).id);
				return true;
			case PlaybackCodes.MENU_MOVE_DOWN:
				moveQueueItemDownById(((AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo()).id);
				return true;
			default:
				return super.onContextItemSelected(menuItem);
		}
	}

	// GUI.

	private void wireGui (final View rootView, final ViewGroup container) {
		this.adapter = new QueueCursorAdapter(container.getContext());

		this.queueList = (ListView) rootView.findViewById(R.id.lstQueue);
		this.queueList.setAdapter(this.adapter);
		this.queueList.setOnItemClickListener(this.queueItemClickListener);
		this.queueList.setOnCreateContextMenuListener(this.queueContextMenuListener);
		this.queueList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		this.queueList.setMultiChoiceModeListener(this.queueListMultiChoiceModeListener);

		final View tagRow = rootView.findViewById(R.id.tagRow);
//		tagRow.setOnClickListener(this.contextMenuClickListener);
//		tagRow.setOnCreateContextMenuListener(this.tagRowContextMenuListener);

		this.txtTitle = (TextView) rootView.findViewById(R.id.txtTitle);
		this.txtTags = (TextView) rootView.findViewById(R.id.txtTags);
		this.txtQueue = (TextView) rootView.findViewById(R.id.txtQueue);

		rootView.findViewById(R.id.btnSearch).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				search();
			}
		});
		this.btnPlayPause = rootView.findViewById(R.id.btnPlaypause);
		this.btnPlayPause.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				playpause();
			}
		});
		this.btnPlayPause.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick (final View v) {
				showStopMenu();
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

	private final MediaWatcher mediaWatcher = new MediaWatcherAdapter() {
		@Override
		public void queueChanged () {
			PlayerFragment.this.messageHandler.sendEmptyMessage(Msgs.QUEUE_CHANGED.ordinal());
		}
	};

	private final PlaybackWatcher playbackWatcher = new PlaybackWatcherAdapter() {
		@Override
		public void playbackLoading (final QueueItem item) {
			final Message msg = PlayerFragment.this.messageHandler.obtainMessage(Msgs.PLAYBACK_LOADING.ordinal());
			msg.obj = item;
			msg.sendToTarget();
		}

		@Override
		public void playOrderChanged () {
			PlayerFragment.this.messageHandler.sendEmptyMessage(Msgs.PLAYORDER_CHANGED.ordinal());
		}
	};

	protected enum Msgs {
		QUEUE_CHANGED,
		PLAYORDER_CHANGED,
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
				reloadQueue();
				redrawQueueStatus();
				break;
			case PLAYORDER_CHANGED:
				redrawQueueStatus();
				break;
			case PLAYBACK_LOADING:
				final QueueItem item = (QueueItem) msg.obj;
				redrawCurrentItem(item);
				break;
			default:
		}
	}

	private void redrawCurrentItem (final QueueItem queueItem) {
		this.txtTitle.setText(queueItem.getTitle());

		if (queueItem.hasLibraryId()) {
			final long mediaRowId = getMediaDb().getMediaRowId(queueItem.getLibraryId(), queueItem.getUri());
			if (mediaRowId >= 0) {
				final Map<Long, Collection<MediaTag>> tagMap = getMediaDb().readTags(Collections.singleton(mediaRowId));
				final Collection<MediaTag> tags = tagMap.get(mediaRowId);
				if (tags.size() > 0) {
					final StringBuilder str = new StringBuilder();
					for (final MediaTag tag : tags) {
						if (str.length() > 0) str.append(", ");
						str.append(tag.getTag());
						if (tag.isDeleted()) str.append("(d)");
					}
					this.txtTags.setText(str.toString());
				}
				else {
					this.txtTags.setText("(no tags)");
				}
			}
			else {
				this.txtTags.setText("(item missing from library)");
			}
		}
		else {
			this.txtTags.setText("(item not in library)");
		}
	}

	// Queue.

	private void redrawQueueStatus () {
		this.txtQueue.setText(String.format(
				"%s items in queue, %s.",
				getMediaDb().getQueueSize(),
				getPlaybacker().getPlayOrder()));
	}

	private void reloadQueue () {
		new LoadQueue(this).execute(); // TODO OnExecutor?
	}

	private static class LoadQueue extends AsyncTask<Void, Void, Result<Cursor>> {

		private final PlayerFragment host;

		public LoadQueue (final PlayerFragment host) {
			this.host = host;
		}

		@Override
		protected void onPreExecute () {
			// TODO show progress indicator.
		}

		@Override
		protected Result<Cursor> doInBackground (final Void... params) {
			try {
				final MediaDb db = this.host.getMediaDb();
				if (db != null) {
					final Cursor cursor = db.getQueueCursor();
					return new Result<Cursor>(cursor);
				}
				return new Result<Cursor>(new IllegalStateException("Failed to refresh queue as DB was not bound."));
			}
			catch (final Exception e) { // NOSONAR needed to report errors.
				return new Result<Cursor>(e);
			}
		}

		@Override
		protected void onPostExecute (final Result<Cursor> result) {
			if (result.isSuccess()) {
				this.host.saveScrollIfNotSaved();
				this.host.getAdapter().changeCursor(result.getData());
				LOG.d("Refreshed queue cursor.");
				this.host.restoreScroll();
			}
			else {
				LOG.w("Failed to refresh column.", result.getE());
			}
			// TODO hide progress indicator.
		}

	}

	// Queue scrolling.

	private ScrollState getCurrentScroll () {
		return ScrollState.from(this.queueList);
	}

	private void saveScroll () {
		final ScrollState newState = getCurrentScroll();
		if (newState != null) {
			this.scrollState = newState;
			LOG.d("Saved scroll: %s", this.scrollState);
		}
	}

	private void saveScrollIfNotSaved () {
		if (this.scrollState != null) return;
		saveScroll();
	}

	private void restoreScroll () {
		if (this.scrollState == null) return;
		this.scrollState.applyTo(this.queueList);
		LOG.d("Restored scroll: %s", this.scrollState);
		this.scrollState = null;
	}


	private void addUriToQueue (final Uri mediaUri) {
		final Playbacker pb = getPlaybacker();
		if (pb != null) {
			final QueueItem item = new QueueItem(getActivity(), mediaUri);
			getMediaDb().addToQueue(Collections.singleton(item), QueueEnd.TAIL);
			LOG.i("Added to queue: %s", item);
		}
	}

	private final OnItemClickListener queueItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			// TODO make queue item dlg.
			// TODO move top / move bottom.
			parent.showContextMenuForChild(view);
		}
	};

	private final OnCreateContextMenuListener queueContextMenuListener = new OnCreateContextMenuListener() {
		@Override
		public void onCreateContextMenu (final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			final QueueItem item = getMediaDb().getQueueItemById(info.id);
			menu.setHeaderTitle(item.getTitle());
			menu.add(Menu.NONE, PlaybackCodes.MENU_MOVE_TOP, Menu.NONE, "Move Top");
			menu.add(Menu.NONE, PlaybackCodes.MENU_MOVE_UP, Menu.NONE, "Move Up");
			menu.add(Menu.NONE, PlaybackCodes.MENU_MOVE_DOWN, Menu.NONE, "Move Down");
			menu.add(Menu.NONE, PlaybackCodes.MENU_MOVE_BOTTOM, Menu.NONE, "Move Bottom");
		}
	};

	private final MultiChoiceModeListener queueListMultiChoiceModeListener = new MultiChoiceModeListener() {

		private final Map<Integer, Long> selectedIds = new TreeMap<Integer, Long>();

		@Override
		public boolean onCreateActionMode (final ActionMode mode, final Menu menu) {
			menu.add("Remove").setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick (final MenuItem item) {
					removeFromQueueById(selectedIds.values());
					mode.finish();
					return true;
				}
			});
			return true;
		}

		@Override
		public boolean onPrepareActionMode (final ActionMode mode, final Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked (final ActionMode mode, final MenuItem item) {
			return false; // Items have their only click handlers.
		}

		@Override
		public void onDestroyActionMode (final ActionMode mode) {
			this.selectedIds.clear();
		}

		@Override
		public void onItemCheckedStateChanged (final ActionMode mode, final int position, final long id, final boolean checked) {
			if (checked) {
				this.selectedIds.put(position, id);
			}
			else {
				this.selectedIds.remove(position);
			}
			mode.setTitle(String.format("%s items", this.selectedIds.size()));
		}
	};

	private void moveQueueItemTopById (final long itemId) {
		final MediaDb db = getMediaDb();
		if (db == null) return;
		db.moveQueueItemToEnd(itemId, MoveAction.UP);
	}

	private void moveQueueItemBottomById (final long itemId) {
		final MediaDb db = getMediaDb();
		if (db == null) return;
		db.moveQueueItemToEnd(itemId, MoveAction.DOWN);
	}

	private void moveQueueItemUpById (final long itemId) {
		final MediaDb db = getMediaDb();
		if (db == null) return;
		db.moveQueueItem(itemId, MoveAction.UP);
	}

	private void moveQueueItemDownById (final long itemId) {
		final MediaDb db = getMediaDb();
		if (db == null) return;
		db.moveQueueItem(itemId, MoveAction.DOWN);
	}

	private void removeFromQueueById (final Collection<Long> itemIds) {
		final MediaDb db = getMediaDb();
		if (db == null) return;
		db.removeFromQueueById(itemIds);
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

	private void stopAfter () {
		final Playbacker pb = getPlaybacker();
		if (pb != null) {
			final QueueItem item = new QueueItem(getActivity(), QueueItemType.STOP);
			getMediaDb().addToQueue(Collections.singleton(item), QueueEnd.HEAD);
		}
	}

	private void showStopMenu () {
		final PopupMenu menu = new PopupMenu(getActivity(), this.btnPlayPause);

		menu.getMenu().add(Menu.NONE, Menu.NONE, Menu.NONE, "Stop")
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick (final MenuItem item) {
				stop();
				return true;
			}
		});

		menu.getMenu().add(Menu.NONE, Menu.NONE, Menu.NONE, "Stop After")
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick (final MenuItem item) {
				stopAfter();
				return true;
			}
		});

		menu.show();
	}

}
