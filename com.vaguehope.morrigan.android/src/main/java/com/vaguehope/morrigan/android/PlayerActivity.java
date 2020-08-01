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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import com.vaguehope.morrigan.android.helper.TimeHelper;
import com.vaguehope.morrigan.android.model.Artifact;
import com.vaguehope.morrigan.android.model.ArtifactListAdaptor;
import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.MlistReference;
import com.vaguehope.morrigan.android.model.PlayerQueue;
import com.vaguehope.morrigan.android.model.PlayerQueueChangeListener;
import com.vaguehope.morrigan.android.model.PlayerReference;
import com.vaguehope.morrigan.android.model.PlayerState;
import com.vaguehope.morrigan.android.model.PlayerStateChangeListener;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.modelimpl.ArtifactListAdaptorImpl;
import com.vaguehope.morrigan.android.modelimpl.MlistReferenceImpl;
import com.vaguehope.morrigan.android.modelimpl.PlayerReferenceImpl;
import com.vaguehope.morrigan.android.playback.MediaTag;
import com.vaguehope.morrigan.android.state.ConfigDb;
import com.vaguehope.morrigan.android.tasks.DownloadMediaTask;
import com.vaguehope.morrigan.android.tasks.GetPlayerQueueTask;
import com.vaguehope.morrigan.android.tasks.GetPlayerQueueTask.QueueAction;
import com.vaguehope.morrigan.android.tasks.GetPlayerQueueTask.QueueItemAction;
import com.vaguehope.morrigan.android.tasks.SetPlaystateTask;
import com.vaguehope.morrigan.android.tasks.SetPlaystateTask.TargetPlayState;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerActivity extends Activity implements PlayerStateChangeListener, PlayerQueueChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String SERVER_ID = "serverId"; // int.
	public static final String PLAYER_ID = "playerId"; // int.

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	ConfigDb configDb;
	protected ServerReference serverReference = null;
	protected PlayerReference playerReference = null;
	protected PlayerState currentState;
	protected ArtifactListAdaptor<PlayerQueue> queueListAdaptor;
	protected MlistReference mlistReference = null;

	private ErrorsList errorsList;

	private final AtomicReference<String> lastQuery = new AtomicReference<String>();

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.configDb = new ConfigDb(this);

		Bundle extras = getIntent().getExtras();
		String serverId = extras.getString(SERVER_ID);
		String playerId = extras.getString(PLAYER_ID);

		if (serverId != null && playerId != null) {
			this.serverReference = this.configDb.getServer(serverId);
			this.playerReference = new PlayerReferenceImpl(this.serverReference, playerId);
		}
		else {
			finish();
		}

		this.setTitle(this.playerReference.getTitle());

		// TODO check return value.
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.player);
		wireGui();
	}

	@Override
	protected void onStart () {
		super.onStart();
		this.refreshTimer.start(); // Executes immediately then at fixed interval.
	}

	@Override
	protected void onPause () {
		this.refreshTimer.stop();
		super.onPause();
	}

	private final RepeatTimer refreshTimer = new RepeatTimer(C.PLAYER_REFRESH_SECONDS) {
		@Override
		public void timer () {
			refresh();
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Buttons.

	private void wireGui () {
		ListView lstErrors = (ListView) findViewById(R.id.lstErrors);
		this.errorsList = new ErrorsList(this, lstErrors);

		this.queueListAdaptor = new ArtifactListAdaptorImpl<PlayerQueue>(this, R.layout.mlistitemlistrow);
		ListView lstQueue = (ListView) findViewById(R.id.lstQueue);
		lstQueue.setAdapter(this.queueListAdaptor);
		lstQueue.setOnItemClickListener(this.contextMenuItemCickListener);
		lstQueue.setOnCreateContextMenuListener(this.queueContextMenuListener);

		View tagRow = findViewById(R.id.tagRow);
		tagRow.setOnClickListener(this.contextMenuClickListener);
		tagRow.setOnCreateContextMenuListener(this.tagRowContextMenuListener);

		findViewById(R.id.btnPlaypause).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				playpause();
			}
		});
		findViewById(R.id.btnNext).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				next();
			}
		});
		findViewById(R.id.btnSearch).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				search();
			}
		});
		findViewById(R.id.btnRefresh).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick (final View v) {
				refresh();
			}
		});
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Context menus.

	private static final int MENU_CTX_ADDTAG = 7;
	private static final int MENU_CTX_TAG = 8;

	private static final int MENU_CTX_MOVETOP = 2;
	private static final int MENU_CTX_MOVEUP = 3;
	private static final int MENU_CTX_REMOVE = 4;
	private static final int MENU_CTX_MOVEDOWN = 5;
	private static final int MENU_CTX_MOVEBOTTOM = 6;

	@Override
	public boolean onContextItemSelected (final MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			case MENU_CTX_ADDTAG:
			case MENU_CTX_TAG:
				onTagRowContextMenu(menuItem);
				return true;

			case MENU_CTX_MOVETOP:
			case MENU_CTX_MOVEUP:
			case MENU_CTX_REMOVE:
			case MENU_CTX_MOVEDOWN:
			case MENU_CTX_MOVEBOTTOM:
				onQueueContextMenu(menuItem);
				return true;

			default:
				return super.onContextItemSelected(menuItem);
		}
	}

	private final OnClickListener contextMenuClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			PlayerState state = PlayerActivity.this.currentState;
			Collection<MediaTag> tags = state == null ? null : state.getTrackTags();
			if (tags == null || tags.size() < 1) {
				addTag();
			}
			else {
				openContextMenu(v);
			}
		}
	};

	private final OnItemClickListener contextMenuItemCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			openContextMenu(view);
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Tag stuff.

	private final OnCreateContextMenuListener tagRowContextMenuListener = new OnCreateContextMenuListener() {
		@Override
		public void onCreateContextMenu (final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			PlayerState state = PlayerActivity.this.currentState;
			if (state != null) {
				menu.setHeaderTitle(state.getTitle());
				menu.add(Menu.NONE, MENU_CTX_ADDTAG, Menu.NONE, "Add tag...");
				if (state.getTrackTags() != null) {
					for (MediaTag tag : state.getTrackTags()) {
						menu.add(Menu.NONE, MENU_CTX_TAG, Menu.NONE, tag.getTag());
					}
				}
			}
			else {
				menu.setHeaderTitle("(No item)");
			}
		}
	};

	private void onTagRowContextMenu (final MenuItem menuItem) {
		if (menuItem.getItemId() == MENU_CTX_ADDTAG) {
			addTag();
		}
		else if (menuItem.getItemId() == MENU_CTX_TAG) {
			CommonDialogs.searchMlist(this, this.currentState, menuItem.getTitle().toString());
		}
	}

	protected void addTag () {
		if (this.currentState != null) {
			if (this.currentState.getItem() != null && this.currentState.getItem().getRelativeUrl() != null) { // TODO figure out why item is not null.
				CommonDialogs.addTag(this, this.mlistReference, this.currentState.getItem(), new Runnable() {
					@Override
					public void run () {
						refresh();
					}
				});
			}
			else {
				Toast.makeText(this, "No item selected desu~", Toast.LENGTH_SHORT).show();
			}
		}
		else {
			Toast.makeText(this, "No player selected desu~", Toast.LENGTH_SHORT).show();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queue events.

	private final OnCreateContextMenuListener queueContextMenuListener = new OnCreateContextMenuListener() {
		@Override
		public void onCreateContextMenu (final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			Artifact item = PlayerActivity.this.queueListAdaptor.getInputData().getArtifactList().get(info.position);
			menu.setHeaderTitle(item.getTitle());
			menu.add(Menu.NONE, MENU_CTX_MOVETOP, Menu.NONE, "Move top");
			menu.add(Menu.NONE, MENU_CTX_MOVEUP, Menu.NONE, "Move up");
			menu.add(Menu.NONE, MENU_CTX_REMOVE, Menu.NONE, "Remove");
			menu.add(Menu.NONE, MENU_CTX_MOVEDOWN, Menu.NONE, "Move down");
			menu.add(Menu.NONE, MENU_CTX_MOVEBOTTOM, Menu.NONE, "Move bottom");
		}
	};

	private void onQueueContextMenu (final MenuItem menuItem) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
		Artifact item = PlayerActivity.this.queueListAdaptor.getInputData().getArtifactList().get(info.position);

		QueueItemAction action;
		if (menuItem.getItemId() == MENU_CTX_MOVEUP)
			action = QueueItemAction.UP;
		else if (menuItem.getItemId() == MENU_CTX_MOVEDOWN)
			action = QueueItemAction.DOWN;
		else if (menuItem.getItemId() == MENU_CTX_MOVETOP)
			action = QueueItemAction.TOP;
		else if (menuItem.getItemId() == MENU_CTX_MOVEBOTTOM)
			action = QueueItemAction.BOTTOM;
		else if (menuItem.getItemId() == MENU_CTX_REMOVE)
			action = QueueItemAction.REMOVE;
		else throw new IllegalStateException();

		GetPlayerQueueTask queueTask = new GetPlayerQueueTask(this, this.playerReference, this, action, item);
		queueTask.execute();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Menu items.

	private static final int MENU_FULLSCREEN = 2;
	private static final int MENU_DOWNLOAD = 3;
	private static final int MENU_CLEARQUEUE = 4;
	private static final int MENU_SHUFFLEQUEUE = 5;

	@Override
	public boolean onCreateOptionsMenu (final Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_FULLSCREEN, 0, R.string.menu_fullscreen).setIcon(R.drawable.display);
		menu.add(0, MENU_DOWNLOAD, 0, R.string.menu_download);
		menu.add(0, MENU_CLEARQUEUE, 0, R.string.menu_queue_clear);
		menu.add(0, MENU_SHUFFLEQUEUE, 0, R.string.menu_queue_shuffle);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {

			case MENU_FULLSCREEN:
				fullscreen();
				return true;

			case MENU_DOWNLOAD:
				downloadCurrentItem();
				return true;

			case MENU_CLEARQUEUE:
				new GetPlayerQueueTask(this, this.playerReference, this, QueueAction.CLEAR).execute();
				return true;

			case MENU_SHUFFLEQUEUE:
				new GetPlayerQueueTask(this, this.playerReference, this, QueueAction.SHUFFLE).execute();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.

	protected void refresh () {
		new SetPlaystateTask(this, this.playerReference, this).execute();
		new GetPlayerQueueTask(this, this.playerReference, this).execute();
	}

	protected void playpause () {
		new SetPlaystateTask(this, this.playerReference, TargetPlayState.PLAYPAUSE, this).execute();
	}

	protected void next () {
		new SetPlaystateTask(this, this.playerReference, TargetPlayState.NEXT, this).execute();
	}

	protected void search () {
		if (this.currentState != null) {
			if (this.currentState.getListUrl() != null) {
				CommonDialogs.doSearchMlist(this, this.currentState, this.lastQuery);
			}
			else {
				Toast.makeText(this, "No list selected desu~", Toast.LENGTH_SHORT).show();
			}
		}
		else {
			Toast.makeText(this, "No player selected desu~", Toast.LENGTH_SHORT).show();
		}
	}

	protected void fullscreen () {
		if (this.currentState != null) {
			Map<Integer, String> monitors = this.currentState.getMonitors();
			final List<String> list = new LinkedList<String>();
			for (Entry<Integer, String> monitor : monitors.entrySet()) {
				list.add(monitor.getKey() + ":" + monitor.getValue());
			}
			final String[] labels = list.toArray(new String[list.size()]);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Full-screen");
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick (final DialogInterface dialog, final int which) {
					dialog.dismiss();
				}
			});
			builder.setItems(labels, new DialogInterface.OnClickListener() {
				@Override
				public void onClick (final DialogInterface dialog, final int item) {
					dialog.dismiss();
					new SetPlaystateTask(PlayerActivity.this, PlayerActivity.this.playerReference, item, PlayerActivity.this).execute();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
		}
		else {
			Toast.makeText(this, "Not available.", Toast.LENGTH_SHORT).show();
		}
	}

	protected void downloadCurrentItem () {
		MlistItem item = this.currentState.getItem();
		if (item != null && this.mlistReference != null) {
			new DownloadMediaTask(this, this.mlistReference).execute(item);
		}
		else {
			Toast.makeText(this, "No item to download desu~", Toast.LENGTH_SHORT).show();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	UI updating.

	private static final String FEED_PLAYER = "player";
	private static final String FEED_QUEUE = "queue";

	@Override
	public void onPlayerStateChange (final PlayerState newState, final Exception exception) {
		this.currentState = newState;
		this.errorsList.setError(FEED_PLAYER, exception);
		if (newState != null) {
			if (newState.getName() != null && newState.getName().length() > 0) {
				this.setTitle(this.playerReference.getTitle() + ": " + newState.getName());
			}

			if (newState.getListUrl() != null) {
				this.mlistReference = new MlistReferenceImpl(newState.getListUrl(), this.serverReference);
			}
			else {
				this.mlistReference = null;
			}

			TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
			if (newState.getTrackDuration() > 0) {
				txtTitle.setText(
						newState.getListTitle() + " / "
								+ newState.getTrackTitle() + " ("
								+ TimeHelper.formatTimeSeconds(newState.getTrackDuration()) + ")"
						);
			}
			else {
				txtTitle.setText(newState.getTrackTitle());
			}

			TextView txtTags = (TextView) findViewById(R.id.txtTags);
			final Collection<MediaTag> tagCol = newState.getTrackTags();
			if (tagCol != null && tagCol.size() > 0) {
				final StringBuilder str = new StringBuilder();
				for (final MediaTag tag : tagCol) {
					if (str.length() > 0) str.append(", ");
					str.append(tag.getTag());
					if (tag.isDeleted()) str.append("(d)");
				}
				txtTags.setText(str.toString()); // TODO set max length?
			}
			else {
				txtTags.setText("(click to add tags)");
			}

			TextView txtQueue = (TextView) findViewById(R.id.txtQueue);
			if (newState.getQueueDuration() > 0) {
				txtQueue.setText(newState.getQueueLength() + " items, " + TimeHelper.formatTimeSeconds(newState.getQueueDuration()) + ".");
			}
			else {
				txtQueue.setText(newState.getQueueLength() + " items.");
			}

			ImageView imgPlaystate = (ImageView) findViewById(R.id.imgPlaystate);
			imgPlaystate.setImageResource(newState.getImageResource());
		}
	}

	@Override
	public void onPlayerQueueChange (final PlayerQueue newQueue, final Exception exception) {
		if (newQueue != null) this.queueListAdaptor.setInputData(newQueue);
		this.errorsList.setError(FEED_QUEUE, exception);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
