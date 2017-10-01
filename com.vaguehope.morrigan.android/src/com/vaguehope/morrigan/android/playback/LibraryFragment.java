package com.vaguehope.morrigan.android.playback;

import java.lang.ref.WeakReference;
import java.util.Collection;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.LogWrapper;
import com.vaguehope.morrigan.android.helper.Result;
import com.vaguehope.morrigan.android.playback.MediaDb.MediaWatcher;
import com.vaguehope.morrigan.android.playback.MediaDb.SortColumn;
import com.vaguehope.morrigan.android.playback.MediaDb.SortDirection;

public class LibraryFragment extends Fragment {

	private static final LogWrapper LOG = new LogWrapper("LF");

	private MessageHandler messageHandler;

	private ArrayAdapter<LibraryMetadata> librariesAdaptor;
	private Spinner librariesSelector;
	private ListView mediaList;
	private MediaListCursorAdapter adapter;
	private ScrollState scrollState;

	@Override
	public View onCreateView (final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
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
					getMediaDb().addMediaWatcher(getMediaWatcher());
					LOG.d("Service bound.");
				}
			});
		}
		else if (getMediaDb() != null) { // because we stop listening in onPause(), we must resume if the user comes back.
			getMediaDb().addMediaWatcher(getMediaWatcher());
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
			ms.getMediaDb().removeMediaWatcher(getMediaWatcher());
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

		this.librariesAdaptor = new ArrayAdapter<LibraryMetadata>(container.getContext(), android.R.layout.simple_list_item_1);

		this.librariesSelector = (Spinner) rootView.findViewById(R.id.librariesSelector);
		this.librariesSelector.setAdapter(this.librariesAdaptor);
		this.librariesSelector.setOnItemSelectedListener(this.librarySelectedListener);
		this.librariesSelector.setSelection(0);

		this.mediaList = (ListView) rootView.findViewById(R.id.mediaList);
		this.mediaList.setAdapter(this.adapter);
	}

	protected MediaWatcher getMediaWatcher () {
		return this.mediaWatcher;
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
				refreshLibraries();
				break;
			case LIBRARY_CHANGED:
				// TODO check is the selected library that changed?
				reloadLibrary();
				break;
			default:
		}
	}

	private void refreshLibraries () {
		final Collection<LibraryMetadata> dbs = getMediaDb().getLibraries();
		this.librariesAdaptor.clear();
		this.librariesAdaptor.addAll(dbs);
	}

	private final OnItemSelectedListener librarySelectedListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected (final AdapterView<?> parent, final View view, final int position, final long id) {
			final LibraryMetadata library = LibraryFragment.this.librariesAdaptor.getItem(position);
			setCurrentLibrary(library);
		}

		@Override
		public void onNothingSelected (final AdapterView<?> parent) {}

	};

	private LibraryMetadata currentLibrary;

	private void setCurrentLibrary (final LibraryMetadata library) {
		this.currentLibrary = library;
		reloadLibrary();
	}

	private void reloadLibrary () {
		new LoadLibrary(this, this.currentLibrary, SortColumn.PATH, SortDirection.ASC).execute(); // TODO OnExecutor?
	}

	private static class LoadLibrary extends AsyncTask<Void, Void, Result<Cursor>> {

		private final LibraryFragment host;
		private final LibraryMetadata library;
		private final SortColumn sortColumn;
		private final SortDirection sortDirection;

		public LoadLibrary (final LibraryFragment host, final LibraryMetadata library, final SortColumn sortColumn, final SortDirection sortDirection) {
			this.host = host;
			this.library = library;
			this.sortColumn = sortColumn;
			this.sortDirection = sortDirection;
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
					final Cursor cursor = db.getAllMediaCursor(this.library.getId(), this.sortColumn, this.sortDirection);
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
			}
			else {
				LOG.w("Failed to refresh column.", result.getE());
			}
			// TODO hide progress indicator.
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
