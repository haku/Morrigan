package com.vaguehope.morrigan.android.playback;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.IoHelper;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.helper.Result;
import com.vaguehope.morrigan.android.playback.MediaDb.MediaWatcher;
import com.vaguehope.morrigan.android.playback.MediaDb.QueueEnd;
import com.vaguehope.morrigan.android.playback.MediaDb.SortColumn;
import com.vaguehope.morrigan.android.playback.MediaDb.SortDirection;

public class LibraryFragment extends Fragment {

	private static final LogWrapper LOG = new LogWrapper("LF");

	private static final int MENU_LIBRARY_ENQUEUE_ALL = 1050;
	private static final int MENU_LIBRARY_ID_START = 1100;
	private static final int MENU_LIBRARY_COLUMN_START = 1200;
	private static final int MENU_LIBRARY_DIRECTION_START = 1300;

	// Intent params.
	private int fragmentPosition;

	private PlaybackActivity playbackActivity;
	private MessageHandler messageHandler;

	private EditText txtSearch;
	private TextView txtResultsInfo;
	private Button btnLibrary;
	private ListView mediaList;

	private MediaListCursorAdapter adapter;
	private ScrollState scrollState;

	private Collection<LibraryMetadata> allLibraries;
	private PopupMenu libraryMenu;
	private LibraryMetadata currentLibrary;
	private SortColumn currentSortColumn = SortColumn.PATH;
	private SortDirection currentSortDirection = SortDirection.ASC;

	@Override
	public View onCreateView (final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		this.fragmentPosition = getArguments().getInt(SectionsPagerAdapter.ARG_FRAGMENT_POSITION, -1);
		if (this.fragmentPosition < 0) throw new IllegalArgumentException("Missing fragmentPosition.");

		this.playbackActivity = (PlaybackActivity) getActivity();
		this.messageHandler = new MessageHandler(this);

		final View rootView = inflater.inflate(R.layout.playback_library, container, false);
		wireGui(rootView, container);
		return rootView;
	}

	@Override
	public void onResume () {
		super.onResume();
		resumeMc();
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

	private PlaybackActivity getPlaybackActivity() {
		return this.playbackActivity;
	}

	// Service.

	private MediaClient bndMc;

	private void resumeMc () {
		if (this.bndMc == null) {
			LOG.d("Binding service...");
			this.bndMc = new MediaClient(getActivity(), LOG.getPrefix(), new Runnable() {
				@Override
				public void run () {
					/*
					 * this convoluted method is because the service connection
					 * won't finish until this thread processes messages again
					 * (i.e., after it exits this thread). if we try to talk to
					 * the DB service before then, it will NPE.
					 */
					getMediaDb().addMediaWatcher(LibraryFragment.this.mediaWatcher);
					LOG.d("Service bound.");
				}
			});
		}
		else if (getMediaDb() != null) { // because we stop listening in onPause(), we must resume if the user comes back.
			getMediaDb().addMediaWatcher(this.mediaWatcher);
			LOG.d("Service rebound.");
		}
		else {
			LOG.w("resumeMc() called while service is half bound.  I do not know what to do.");
		}
	}

	private void suspendDb () {
		// We might be pausing before the callback has come.
		final MediaServices ms = this.bndMc.getService();
		if (ms != null) { // We might be pausing before the callback has come.
			ms.getMediaDb().removeMediaWatcher(this.mediaWatcher);
		}
		else { // If we have not even had the callback yet, cancel it.
			this.bndMc.clearReadyListener();
		}
		LOG.d("Service released.");
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

	protected MediaListCursorAdapter getAdapter () {
		return this.adapter;
	}

	// GUI.

	private void wireGui (final View rootView, final ViewGroup container) {
		this.adapter = new MediaListCursorAdapter(container.getContext());

		this.txtSearch = (EditText) rootView.findViewById(R.id.txtSearch);
		this.txtSearch.setImeActionLabel("Search", KeyEvent.KEYCODE_ENTER);
		this.txtSearch.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction (final TextView v, final int actionId, final KeyEvent event) {
				reloadLibrary();
				return true;
			}
		});

		this.txtResultsInfo = (TextView) rootView.findViewById(R.id.txtResultsInfo);

		this.btnLibrary = (Button) rootView.findViewById(R.id.btnLibrary);
		this.btnLibrary.setOnClickListener(this.btnLibraryOnClickListener);

		this.mediaList = (ListView) rootView.findViewById(R.id.mediaList);
		this.mediaList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		this.mediaList.setMultiChoiceModeListener(this.mediaListMultiChoiceModeListener);
		this.mediaList.setOnItemClickListener(this.mediaListOnItemClickListener);
		this.mediaList.setAdapter(this.adapter);
	}

	private final MediaWatcher mediaWatcher = new MediaWatcherAdapter() {
		@Override
		public void librariesChanged () {
			LibraryFragment.this.messageHandler.sendEmptyMessage(Msgs.LIBRARIES_CHANGED.ordinal());
		}
	};

	protected enum Msgs {
		LIBRARIES_CHANGED,
		LIBRARY_CHANGED;
		public static final Msgs values[] = values(); // Optimisation to avoid new array every time.
	}

	private static class MessageHandler extends Handler {

		private final WeakReference<LibraryFragment> parentRef;

		public MessageHandler (final LibraryFragment libraryFragment) {
			this.parentRef = new WeakReference<LibraryFragment>(libraryFragment);
		}

		@Override
		public void handleMessage (final Message msg) {
			final LibraryFragment parent = this.parentRef.get();
			if (parent != null) parent.msgOnUiThread(msg);
		}
	}

	protected void msgOnUiThread (final Message msg) {
		final Msgs m = Msgs.values[msg.what];
		switch (m) {
			case LIBRARIES_CHANGED:
				onLibrariesChanged();
				break;
			case LIBRARY_CHANGED:
				// TODO check is the selected library that changed?
				reloadLibrary();
				break;
			default:
		}
	}

	private final OnClickListener btnLibraryOnClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			final PopupMenu menu = LibraryFragment.this.libraryMenu;
			if (menu != null) menu.show();
		}
	};

	private final MultiChoiceModeListener mediaListMultiChoiceModeListener = new MultiChoiceModeListener() {

		private final Map<Integer, Long> selectedIds = new TreeMap<Integer, Long>();

		@Override
		public boolean onCreateActionMode (final ActionMode mode, final Menu menu) {
			menu.add("Enqueue Top").setOnMenuItemClickListener(new EnqueueActionListener(
					this.selectedIds.values(), QueueEnd.HEAD, mode));
			menu.add("Enqueue").setOnMenuItemClickListener(new EnqueueActionListener(
					this.selectedIds.values(), QueueEnd.TAIL, mode));
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

	private class EnqueueActionListener implements OnMenuItemClickListener {

		private final Collection<Long> selectedIds;
		private final QueueEnd end;
		private final ActionMode mode;

		public EnqueueActionListener (final Collection<Long> collection, final QueueEnd end, final ActionMode mode) {
			this.selectedIds = collection;
			this.end = end;
			this.mode = mode;
		}

		@Override
		public boolean onMenuItemClick (final MenuItem item) {
			addMediaIdsToQueue(this.selectedIds, this.end);
			this.mode.finish();
			return true;
		}

	}

	private final OnItemClickListener mediaListOnItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			final MediaItem mediaItem = getMediaDb().getMediaItem(id);
			if (mediaItem != null) {
				addMediaItemToQueue(mediaItem);
			}
			else {
				LOG.w("Item %s not found in DB.", id);
			}
		}
	};

	private void addMediaIdsToQueue (final Collection<Long> mediaIds, final QueueEnd end) {
		final MediaDb db = getMediaDb();
		final Collection<QueueItem> queueItems = new ArrayList<QueueItem>(mediaIds.size());
		for (final Long mediaId : mediaIds) {
			final MediaItem mediaItem = db.getMediaItem(mediaId);
			if (mediaItem != null) {
				queueItems.add(new QueueItem(mediaItem));
			}
			else {
				LOG.w("Item %s not found in DB.", mediaId);
			}
		}
		db.addToQueue(queueItems, end);
		LOG.i("Added %s items to queue.", queueItems.size());
		Toast.makeText(getActivity(), String.format("Enqueued %s items.", queueItems.size()), Toast.LENGTH_SHORT).show();
	}

	private void addMediaItemToQueue (final MediaItem mediaItem) {
		final QueueItem item = new QueueItem(mediaItem);
		getMediaDb().addToQueue(Collections.singleton(item), QueueEnd.TAIL);
		LOG.i("Added to queue: %s", item);
		Toast.makeText(getActivity(), String.format("Enqueued:\n%s", item.getTitle()), Toast.LENGTH_SHORT).show();
	}

	private void onLibrariesChanged () {
		this.allLibraries = getMediaDb().getLibraries();

		makeLibraryMenu();

		LOG.i("menu reset %s %s", this.currentLibrary, this.allLibraries.size());

		if (this.currentLibrary == null && this.allLibraries.size() > 0) {
			setCurrentLibrary(this.allLibraries.iterator().next());
		}
	}

	private void makeLibraryMenu () {
		final PopupMenu newMenu = new PopupMenu(getActivity(), LibraryFragment.this.btnLibrary);

		{
			final MenuItem item = newMenu.getMenu().add(Menu.NONE, MENU_LIBRARY_ENQUEUE_ALL, Menu.NONE, "Enqueue All");
			item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick (final MenuItem item) {
					enqueueAllSearchResults();
					return true;
				}
			});
		}

		for (final LibraryMetadata library : this.allLibraries) {
			final MenuItem item = newMenu.getMenu().add(Menu.NONE, MENU_LIBRARY_ID_START + (int) library.getId(), Menu.NONE, library.getName());
			item.setCheckable(true);
			item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick (final MenuItem item) {
					setCurrentLibrary(library);
					return true;
				}
			});
		}

		for (final SortColumn s : SortColumn.values()) {
			final MenuItem item = newMenu.getMenu().add(Menu.NONE, MENU_LIBRARY_COLUMN_START + s.ordinal(), Menu.NONE, s.toString());
			item.setCheckable(true);
			item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick (final MenuItem item) {
					setSortColumn(s);
					return true;
				}
			});
		}

		for (final SortDirection s : SortDirection.values()) {
			final MenuItem item = newMenu.getMenu().add(Menu.NONE, MENU_LIBRARY_DIRECTION_START + s.ordinal(), Menu.NONE, s.toString());
			item.setCheckable(true);
			item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick (final MenuItem item) {
					setSortDirection(s);
					return true;
				}
			});
		}

		this.libraryMenu = newMenu;
		updateLibraryMenuSelections();
	}

	private void updateLibraryMenuSelections () {
		final LibraryMetadata library = this.currentLibrary;
		final SortColumn sortColumn = this.currentSortColumn;
		final SortDirection sortDirection = this.currentSortDirection;
		final PopupMenu menu = this.libraryMenu;
		if (library == null || sortColumn == null || sortDirection == null || menu == null) return;

		for (int i = 0; i < menu.getMenu().size(); i++) {
			final MenuItem item = menu.getMenu().getItem(i);
			item.setChecked(item.getItemId() == MENU_LIBRARY_ID_START + library.getId()
					|| item.getItemId() == MENU_LIBRARY_COLUMN_START + sortColumn.ordinal()
					|| item.getItemId() == MENU_LIBRARY_DIRECTION_START + sortDirection.ordinal());
		}
	}

	private void setCurrentLibrary (final LibraryMetadata library) {
		setCurrentLibrary(library, this.currentSortColumn, this.currentSortDirection);
	}

	protected void setSortColumn (final SortColumn sortColumn) {
		setCurrentLibrary(this.currentLibrary, sortColumn, this.currentSortDirection);
	}

	protected void setSortDirection (final SortDirection sortDirection) {
		setCurrentLibrary(this.currentLibrary, this.currentSortColumn, sortDirection);
	}

	private void setCurrentLibrary (final LibraryMetadata library, final SortColumn sortColumn, final SortDirection sortDirection) {
		this.currentLibrary = library;
		this.currentSortColumn = sortColumn;
		this.currentSortDirection = sortDirection;

		reloadLibrary();

		if (library != null) {
			((PlaybackActivity) getActivity()).getSectionsPagerAdapter().setPageTitle(this.fragmentPosition, library.getName());
			this.btnLibrary.setText(library.getName());
		}

		updateLibraryMenuSelections();
	}

	private void reloadLibrary () {
		new LoadLibrary(this, makePendingSearchCursor()).execute(); // TODO OnExecutor?
	}

	private void enqueueAllSearchResults () {
		new EnqueueAllSearchResults(this, makePendingSearchCursor()).execute(); // TODO OnExecutor?
	}

	private PendingSearchCursor makePendingSearchCursor () {
		final String query = this.txtSearch.getText().toString().trim();
		return new PendingSearchCursor(this.currentLibrary, query, this.currentSortColumn, this.currentSortDirection);
	}

	private static class PendingSearchCursor {

		final LibraryMetadata library;
		final String query;
		final SortColumn sortColumn;
		final SortDirection sortDirection;

		public PendingSearchCursor (final LibraryMetadata library, final String query, final SortColumn sortColumn, final SortDirection sortDirection) {
			this.library = library;
			this.query = query;
			this.sortColumn = sortColumn;
			this.sortDirection = sortDirection;
		}

		private Cursor makeCursor (final MediaDb db) {
			return db.searchMediaCursor(this.library.getId(), this.query, this.sortColumn, this.sortDirection);
		}
	}

	private static class LoadLibrary extends AsyncTask<Void, Void, Result<Cursor>> {

		private final LibraryFragment host;
		private final PendingSearchCursor pendingSearchCursor;

		public LoadLibrary (
				final LibraryFragment host,
				final PendingSearchCursor pendingSearchCursor) {
			this.host = host;
			this.pendingSearchCursor = pendingSearchCursor;
		}

		@Override
		protected void onPreExecute () {
			this.host.getPlaybackActivity().progressIndicator(true);
		}

		@Override
		protected Result<Cursor> doInBackground (final Void... params) {
			try {
				final MediaDb db = this.host.getMediaDb();
				if (db != null) {
					final Cursor cursor = this.pendingSearchCursor.makeCursor(db);
					return new Result<Cursor>(cursor);
				}
				return new Result<Cursor>(new IllegalStateException("Failed to refresh column as DB was not bound."));
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
				LOG.d("Refreshed library cursor.");
				this.host.restoreScroll();
				this.host.txtResultsInfo.setText(String.format("%s items.", result.getData().getCount()));
			}
			else {
				LOG.w("Failed to refresh column.", result.getE());
			}
			this.host.getPlaybackActivity().progressIndicator(false);
		}

	}
	private static class EnqueueAllSearchResults extends AsyncTask<Void, Void, Result<Integer>> {

		private final LibraryFragment host;
		private final PendingSearchCursor pendingSearchCursor;

		public EnqueueAllSearchResults (
				final LibraryFragment host,
				final PendingSearchCursor pendingSearchCursor) {
			this.host = host;
			this.pendingSearchCursor = pendingSearchCursor;
		}

		@Override
		protected void onPreExecute () {
			this.host.getPlaybackActivity().progressIndicator(true);
		}

		@Override
		protected Result<Integer> doInBackground (final Void... params) {
			try {
				final MediaDb db = this.host.getMediaDb();
				if (db != null) {
					final Collection<QueueItem> queueItems;
					final Cursor cursor = this.pendingSearchCursor.makeCursor(db);
					try {
						queueItems = new ArrayList<QueueItem>(cursor.getCount());
						if (cursor.moveToFirst()) {
							final MediaCursorReader reader = new MediaCursorReader();
							do {
								// TODO This is perhaps the way it should be done?
								// But ATM it is not needed and less efficient.
								//final MediaItem mediaItem = reader.readItem(cursor);
								//final QueueItem queueItem = new QueueItem(activity, mediaItem);
								final QueueItem queueItem = new QueueItem(this.host.getActivity(),
										this.pendingSearchCursor.library.getId(), reader.readUri(cursor));
								queueItems.add(queueItem);
							}
							while (cursor.moveToNext());
						}
					}
					finally {
						IoHelper.closeQuietly(cursor);
					}
					db.addToQueue(queueItems, QueueEnd.TAIL);
					return new Result<Integer>(queueItems.size());
				}
				return new Result<Integer>(new IllegalStateException("Failed to add to queue as DB was not bound."));
			}
			catch (final Exception e) { // NOSONAR needed to report errors.
				return new Result<Integer>(e);
			}
		}

		@Override
		protected void onPostExecute (final Result<Integer> result) {
			if (result.isSuccess()) {
				LOG.d("All search results added to queue.");
				Toast.makeText(this.host.getActivity(),
						String.format("Added %s items to queue.", result.getData()),
						Toast.LENGTH_SHORT).show();
			}
			else {
				LOG.w("Failed to add search results to queue.", result.getE());
			}
			this.host.getPlaybackActivity().progressIndicator(false);
		}

	}

	// Scrolling.

	private ScrollState getCurrentScroll () {
		return ScrollState.from(this.mediaList);
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
		this.scrollState.applyTo(this.mediaList);
		LOG.d("Restored scroll: %s", this.scrollState);
		this.scrollState = null;
	}

}
