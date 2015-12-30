/*
 * Copyright 2010 Fae Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.vaguehope.morrigan.android;

import java.util.LinkedList;
import java.util.List;

import com.vaguehope.morrigan.android.CommonDialogs.PlayerSelectedListener;
import com.vaguehope.morrigan.android.helper.StringHelper;
import com.vaguehope.morrigan.android.helper.TimeHelper;
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.MlistItemList;
import com.vaguehope.morrigan.android.model.MlistItemListChangeListener;
import com.vaguehope.morrigan.android.model.MlistReference;
import com.vaguehope.morrigan.android.model.MlistState;
import com.vaguehope.morrigan.android.model.MlistStateChangeListener;
import com.vaguehope.morrigan.android.model.PlayerReference;
import com.vaguehope.morrigan.android.model.PlayerState;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.modelimpl.ArtifactListAdaptorImpl;
import com.vaguehope.morrigan.android.modelimpl.MlistItemComparators;
import com.vaguehope.morrigan.android.modelimpl.MlistReferenceImpl;
import com.vaguehope.morrigan.android.modelimpl.PlayerReferenceImpl;
import com.vaguehope.morrigan.android.state.ConfigDb;
import com.vaguehope.morrigan.android.tasks.AbstractTask;
import com.vaguehope.morrigan.android.tasks.BulkRunner;
import com.vaguehope.morrigan.android.tasks.DownloadMediaTask;
import com.vaguehope.morrigan.android.tasks.GetMlistItemListTask;
import com.vaguehope.morrigan.android.tasks.GetMlistTask;
import com.vaguehope.morrigan.android.tasks.RunMlistActionTask;
import com.vaguehope.morrigan.android.tasks.RunMlistActionTask.MlistCommand;
import com.vaguehope.morrigan.android.tasks.RunMlistItemActionTask;
import com.vaguehope.morrigan.android.tasks.RunMlistItemActionTask.MlistItemCommand;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MlistActivity extends Activity implements MlistStateChangeListener, MlistItemListChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String SERVER_ID = "serverId"; // int.
	public static final String MLIST_BASE_URL = "mlistBaseUrl"; // String.
	public static final String PLAYER_ID = "playerId"; // int.

	public static final String QUERY = "query";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	ConfigDb configDb;
	private ErrorsList errorsList;
	protected ServerReference serverReference = null;
	protected MlistReference mlistReference = null;
	protected ArtifactListAdaptorImpl<MlistItemList> mlistItemListAdapter;
	private MlistState currentState = null;
	private MlistItemList currentItemList = null;
	private String currentQuery = null;
	protected PlayerReference playerReference;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.configDb = new ConfigDb(this);

		Bundle extras = getIntent().getExtras();
		String serverId = extras.getString(SERVER_ID);
		String mlistBaseUrl = extras.getString(MLIST_BASE_URL);
		String playerId = extras.getString(PLAYER_ID);
		this.currentQuery = extras.getString(QUERY);

		if (serverId != null && mlistBaseUrl != null) {
			this.serverReference = this.configDb.getServer(serverId);
			this.mlistReference = new MlistReferenceImpl(mlistBaseUrl, this.serverReference);
		}
		else {
			finish();
		}

		if (playerId != null) {
			this.playerReference = new PlayerReferenceImpl(this.serverReference, playerId);
		}

		this.setTitle(this.mlistReference.getBaseUrl());

		// TODO check return value.
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.mlist);
		wireGui();
	}

	@Override
	protected void onStart () {
		super.onStart();
		refresh();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI setup and buttons.

	private void wireGui () {
		ListView lstErrors = (ListView) findViewById(R.id.lstErrors);
		this.errorsList = new ErrorsList(this, lstErrors);

		this.mlistItemListAdapter = new ArtifactListAdaptorImpl<MlistItemList>(this, R.layout.mlistitemlistrow);

		ListView lstItems = (ListView) findViewById(R.id.lstItems);
		lstItems.setAdapter(this.mlistItemListAdapter);
		lstItems.setOnItemClickListener(this.mlistItemListCickListener);
		lstItems.setOnCreateContextMenuListener(this.itemsContextMenuListener);

		((ImageButton) findViewById(R.id.btnQueue)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				queueAll();
			}
		});

		((ImageButton) findViewById(R.id.btnSearch)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				search();
			}
		});

		((ImageButton) findViewById(R.id.btnRefresh)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				refresh();
			}
		});
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	View menu.

	private static final int MENU_SCAN = 1;
	private static final int MENU_DOWNLOAD = 2;
	private static final int MENU_SORT = 3;

	@Override
	public boolean onCreateOptionsMenu (final Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_SCAN, 0, R.string.menu_scan);
		menu.add(0, MENU_DOWNLOAD, 1, R.string.menu_download);
		menu.add(0, MENU_SORT, 1, R.string.menu_sort);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {

			case MENU_SCAN:
				scan();
				return true;

			case MENU_DOWNLOAD:
				downloadAllInList();
				return true;

			case MENU_SORT:
				this.mlistItemListAdapter.getInputData().sort(MlistItemComparators.FILENAME);
				this.mlistItemListAdapter.notifyDataSetChanged();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Context menu.

	private static final int MENU_CTX_PLAY = 1;
	private static final int MENU_CTX_QUEUE = 2;
	private static final int MENU_CTX_ADDTAG = 3;
	private static final int MENU_CTX_TAGS = 5;
	private static final int MENU_CTX_TAG = 6;
	private static final int MENU_CTX_DOWNLOAD = 4;

	private final OnItemClickListener mlistItemListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			openContextMenu(view);
		}
	};

	private final OnCreateContextMenuListener itemsContextMenuListener = new OnCreateContextMenuListener() {
		@Override
		public void onCreateContextMenu (final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			MlistItem mlistItem = MlistActivity.this.mlistItemListAdapter.getInputData().getMlistItemList().get(info.position);

			LinearLayout header = new LinearLayout(MlistActivity.this);
			header.setOrientation(LinearLayout.VERTICAL);
			header.setPadding(10, 10, 10, 10);

			TextView title = new TextView(MlistActivity.this);
			title.setText(mlistItem.getFileName());
			title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			header.addView(title);

			TextView counts = new TextView(MlistActivity.this);
			counts.setText(mlistItem.getStartCount() + "/" + mlistItem.getEndCount() + " " + TimeHelper.formatTimeSeconds(mlistItem.getDuration()));
			counts.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			header.addView(counts);

			String[] tarArr = mlistItem.getTags();
			if (tarArr != null && tarArr.length > 0) {
				TextView tags = new TextView(MlistActivity.this);
				tags.setText(StringHelper.implode(tarArr, ", ")); // TODO set max length?
				tags.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				header.addView(tags);
			}

			menu.setHeaderView(header);
			menu.add(Menu.NONE, MENU_CTX_PLAY, Menu.NONE, "Play now");
			menu.add(Menu.NONE, MENU_CTX_QUEUE, Menu.NONE, "Queue");
			menu.add(Menu.NONE, MENU_CTX_ADDTAG, Menu.NONE, "Add tag...");

			if (tarArr != null && tarArr.length > 0) {
				SubMenu tagMenu = menu.addSubMenu(Menu.NONE, MENU_CTX_TAGS, Menu.NONE, "tags...");
				for (String tag : tarArr) {
					tagMenu.add(Menu.NONE, MENU_CTX_TAG, Menu.NONE, tag);
				}
			}

			menu.add(Menu.NONE, MENU_CTX_DOWNLOAD, Menu.NONE, "Download");
		}
	};

	@Override
	public boolean onContextItemSelected (final MenuItem item) {
		MlistItem mlistItem = null;
		switch (item.getItemId()) {
			case MENU_CTX_PLAY:
			case MENU_CTX_QUEUE:
			case MENU_CTX_ADDTAG:
			case MENU_CTX_DOWNLOAD:
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
				mlistItem = MlistActivity.this.mlistItemListAdapter.getInputData().getMlistItemList().get(info.position);
		}

		switch (item.getItemId()) {
			case MENU_CTX_PLAY:
				playItem(mlistItem);
				return true;

			case MENU_CTX_QUEUE:
				queueItem(mlistItem);
				return true;

			case MENU_CTX_ADDTAG:
				CommonDialogs.addTag(this, this.mlistReference, mlistItem, new Runnable() {
					@Override
					public void run () {
						refresh();
					}
				});
				return true;

			case MENU_CTX_TAG:
				search(item.getTitle().toString());
				return true;

			case MENU_CTX_DOWNLOAD:
				DownloadMediaTask task = new DownloadMediaTask(this, this.mlistReference);
				task.execute(mlistItem);
				return true;

			default:
				return super.onContextItemSelected(item);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.

	protected void refresh () {
		new GetMlistTask(this, this.mlistReference, this).execute();
		new GetMlistItemListTask(
				MlistActivity.this, MlistActivity.this.mlistReference,
				MlistActivity.this, this.currentQuery).execute();
	}

	protected void scan () {
		new RunMlistActionTask(this, this.mlistReference, MlistCommand.SCAN).execute();
	}

	protected void search () {
		if (this.currentState == null) return; // TODO show msg here?

		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
		dlgBuilder.setTitle("Query " + this.currentState.getTitle());

		final EditText editText = new EditText(this);
		editText.setSelectAllOnFocus(true);
		if (this.currentItemList != null) editText.setText(this.currentItemList.getQuery());
		dlgBuilder.setView(editText);

		dlgBuilder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				String query = editText.getText().toString().trim();
				dialog.dismiss();
				search(query);
			}
		});

		dlgBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				dialog.cancel();
			}
		});

		dlgBuilder.show();
	}

	protected void search (final String query) {
		new GetMlistItemListTask(MlistActivity.this, MlistActivity.this.mlistReference, MlistActivity.this, query).execute();
	}

	protected void play () {
		if (this.playerReference == null) {
			CommonDialogs.doAskWhichPlayer(MlistActivity.this, MlistActivity.this.serverReference, new PlayerSelectedListener() {
				@Override
				public void playerSelected (final PlayerState playerState) {
					new RunMlistActionTask(MlistActivity.this, MlistActivity.this.mlistReference, MlistCommand.PLAY, playerState.getPlayerReference()).execute();
				}
			});
		}
		else {
			new RunMlistActionTask(this, this.mlistReference, MlistCommand.PLAY, this.playerReference).execute();
		}
	}

	protected void queueAll () {
		int count = this.mlistItemListAdapter.getCount();
		if (count < 1) {
			Toast.makeText(this, "No items to queue", Toast.LENGTH_SHORT).show();
			return;
		}

		AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
		dlgBuilder.setMessage("Queue all " + count + " items?");

		dlgBuilder.setPositiveButton("Queue all", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int which) {
				if (MlistActivity.this.playerReference == null) {
					CommonDialogs.doAskWhichPlayer(MlistActivity.this, MlistActivity.this.serverReference, new PlayerSelectedListener() {
						@Override
						public void playerSelected (final PlayerState playerState) {
							queueItems(MlistActivity.this.mlistItemListAdapter.getInputData().getMlistItemList(), playerState.getPlayerReference());
						}
					});
				}
				else {
					queueItems(MlistActivity.this.mlistItemListAdapter.getInputData().getMlistItemList(), MlistActivity.this.playerReference);
				}
			}
		});

		dlgBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				dialog.cancel();
			}
		});

		dlgBuilder.show();
	}

	protected void downloadAllInList () {
		List<? extends MlistItem> mitems = this.currentItemList.getMlistItemList();
		if (mitems.size() > 0) {
			new DownloadMediaTask(this, this.mlistReference).execute(mitems.toArray(new MlistItem[] {}));
		}
		else {
			Toast.makeText(this, "No items to download desu~", Toast.LENGTH_SHORT).show();
		}
	}

	protected void playItem (final MlistItem item) {
		if (MlistActivity.this.playerReference == null) {
			CommonDialogs.doAskWhichPlayer(this, this.serverReference, new PlayerSelectedListener() {
				@Override
				public void playerSelected (final PlayerState playerState) {
					playItem(item, playerState.getPlayerReference());
				}
			});
		}
		else {
			playItem(item, this.playerReference);
		}
	}

	protected void queueItem (final MlistItem item) {
		if (MlistActivity.this.playerReference == null) {
			CommonDialogs.doAskWhichPlayer(this, this.serverReference, new PlayerSelectedListener() {
				@Override
				public void playerSelected (final PlayerState playerState) {
					queueItem(item, playerState.getPlayerReference());
				}
			});
		}
		else {
			queueItem(item, this.playerReference);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void playItem (final MlistItem item, final PlayerReference playerRef) {
		new RunMlistItemActionTask(this, playerRef, this.mlistReference, item, MlistItemCommand.PLAY).execute();
	}

	protected void queueItem (final MlistItem item, final PlayerReference playerRef) {
		new RunMlistItemActionTask(this, playerRef, this.mlistReference, item, MlistItemCommand.QUEUE).execute();
	}

	protected void queueItems (final List<? extends MlistItem> list, final PlayerReference playerRef) {
		List<AbstractTask<String>> tasks = new LinkedList<AbstractTask<String>>();
		for (MlistItem item : list) {
			RunMlistItemActionTask task = new RunMlistItemActionTask(this, playerRef, this.mlistReference, item, MlistItemCommand.QUEUE);
			task.setShowProgress(false);
			tasks.add(task);
		}
		new BulkRunner<String>(this, tasks).execute();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private void updateTitle () {
		String title = this.serverReference.getName();
		if (this.currentState != null && this.currentState.getTitle() != null) {
			title += " / " + this.currentState.getTitle();
		}
		if (this.currentItemList != null && this.currentItemList.getQuery() != null) {
			title += " / " + this.currentItemList.getQuery();
		}
		this.setTitle(title);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String FEED_MLIST = "mlist";
	private static final String FEED_ITEMS = "items";

	@Override
	public void onMlistStateChange (final MlistState newState, final Exception exception) {
		this.currentState = newState;
		this.errorsList.setError(FEED_MLIST, exception);
		if (newState != null) {
			updateTitle();
			TextView txtCount = (TextView) findViewById(R.id.txtTitle);
			txtCount.setText(newState.getCount() + " items, "
					+ (newState.isDurationComplete() ? "" : "> ")
					+ TimeHelper.formatTimeSeconds(newState.getDuration()) + ".");
		}
	}

	@Override
	public void onMlistItemListChange (final MlistItemList mlistItemList, final Exception exception) {
		this.currentItemList = mlistItemList;
		this.errorsList.setError(FEED_ITEMS, exception);
		if (mlistItemList != null) {
			this.currentQuery = mlistItemList.getQuery();
			updateTitle();
			TextView txtSubTitle = (TextView) findViewById(R.id.txtSubTitle);
			txtSubTitle.setText(mlistItemList.getMlistItemList().size() + " results for '" + mlistItemList.getQuery() + "'.");
			this.mlistItemListAdapter.setInputData(mlistItemList);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
