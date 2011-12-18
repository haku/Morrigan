/*
 * Copyright 2010 Alex Hutter
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.vaguehope.morrigan.android.model.impl.ArtifactListAdaptorImpl;
import com.vaguehope.morrigan.android.model.impl.MlistReferenceImpl;
import com.vaguehope.morrigan.android.model.impl.PlayerReferenceImpl;
import com.vaguehope.morrigan.android.state.ConfigDb;
import com.vaguehope.morrigan.android.tasks.DownloadMediaTask;
import com.vaguehope.morrigan.android.tasks.GetPlayerQueueTask;
import com.vaguehope.morrigan.android.tasks.GetPlayerQueueTask.QueueAction;
import com.vaguehope.morrigan.android.tasks.GetPlayerQueueTask.QueueItemAction;
import com.vaguehope.morrigan.android.tasks.SetPlaystateTask;
import com.vaguehope.morrigan.android.tasks.SetPlaystateTask.TargetPlayState;

public class PlayerActivity extends Activity implements PlayerStateChangeListener, PlayerQueueChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String SERVER_ID = "serverId"; // int.
	public static final String PLAYER_ID = "playerId"; // int.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	ConfigDb configDb;
	protected ServerReference serverReference = null;
	protected PlayerReference playerReference = null;
	private PlayerState currentState;
	protected ArtifactListAdaptor<PlayerQueue> queueListAdaptor;
	private MlistReference mlistReference = null;
	
	private AtomicReference<String> lastQuery = new AtomicReference<String>();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.configDb = new ConfigDb(this);
		
		Bundle extras = getIntent().getExtras();
		int serverId = extras.getInt(SERVER_ID, -1);
		int playerId = extras.getInt(PLAYER_ID, -1);
		
		if (serverId >= 0 && playerId >= 0) {
			this.serverReference = this.configDb.getServer(serverId);
			this.playerReference = new PlayerReferenceImpl(this.serverReference, playerId);
		}
		else {
			finish();
		}
		
		this.setTitle(this.playerReference.getBaseUrl());
		
		// TODO check return value.
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.player);
		wireGui();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		refresh();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Buttons.
    
	private void wireGui () {
		this.queueListAdaptor = new ArtifactListAdaptorImpl<PlayerQueue>(this, R.layout.mlistitemlistrow);
		ListView lstQueue = (ListView) findViewById(R.id.lstQueue);
		lstQueue.setAdapter(this.queueListAdaptor);
		lstQueue.setOnItemClickListener(this.queueListCickListener);
		lstQueue.setOnCreateContextMenuListener(this.queueContextMenuListener);
		
		ImageButton cmd;
		
		cmd = (ImageButton) findViewById(R.id.btnPlaypause);
		cmd.setOnClickListener(new BtnPlaypause_OnClick());
		
		cmd = (ImageButton) findViewById(R.id.btnNext);
		cmd.setOnClickListener(new BtnNext_OnClick());
		
		cmd = (ImageButton) findViewById(R.id.btnSearch);
		cmd.setOnClickListener(new BtnSearch_OnClick());
		
		cmd = (ImageButton) findViewById(R.id.btnRefresh);
		cmd.setOnClickListener(new BtnRefresh_OnClick());
	}
	
	class BtnPlaypause_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			playpause();
		}
	}
	
	class BtnNext_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			next();
		}
	}
	
	class BtnSearch_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			search();
		}
	}
	
	class BtnRefresh_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			refresh();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queue events.
	
	private static final int MENU_CTX_MOVETOP = 2;
	private static final int MENU_CTX_MOVEUP = 3;
	private static final int MENU_CTX_REMOVE = 4;
	private static final int MENU_CTX_MOVEDOWN = 5;
	private static final int MENU_CTX_MOVEBOTTOM = 6;
	
	private OnItemClickListener queueListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			openContextMenu(view);
		}
	};
	
	private OnCreateContextMenuListener queueContextMenuListener = new OnCreateContextMenuListener () {
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
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
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			
			case MENU_CTX_MOVETOP:
			case MENU_CTX_MOVEUP:
			case MENU_CTX_REMOVE:
			case MENU_CTX_MOVEDOWN:
			case MENU_CTX_MOVEBOTTOM:
				
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
				Artifact item = PlayerActivity.this.queueListAdaptor.getInputData().getArtifactList().get(info.position);
				
				QueueItemAction action;
				if (menuItem.getItemId() == MENU_CTX_MOVEUP) action = QueueItemAction.UP;
				else if (menuItem.getItemId() == MENU_CTX_MOVEDOWN) action = QueueItemAction.DOWN;
				else if (menuItem.getItemId() == MENU_CTX_MOVETOP) action = QueueItemAction.TOP;
				else if (menuItem.getItemId() == MENU_CTX_MOVEBOTTOM) action = QueueItemAction.BOTTOM;
				else if (menuItem.getItemId() == MENU_CTX_REMOVE) action = QueueItemAction.REMOVE;
				else throw new IllegalStateException();
				
				GetPlayerQueueTask queueTask = new GetPlayerQueueTask(this, this.playerReference, this, action, item);
				queueTask.execute();
				
				return true;
			
			default:
				return super.onContextItemSelected(menuItem);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Menu items.
	
	private static final int MENU_FULLSCREEN = 2;
	private static final int MENU_DOWNLOAD = 3;
	private static final int MENU_CLEARQUEUE = 4;
	private static final int MENU_SHUFFLEQUEUE = 5;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_FULLSCREEN, 0, R.string.menu_fullscreen).setIcon(R.drawable.display);
		menu.add(0, MENU_DOWNLOAD, 0, R.string.menu_download);
		menu.add(0, MENU_CLEARQUEUE, 0, R.string.menu_queue_clear);
		menu.add(0, MENU_SHUFFLEQUEUE, 0, R.string.menu_queue_shuffle);
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			
			case MENU_FULLSCREEN:
				fullscreen();
				return true;
			
			case MENU_DOWNLOAD:
				downloadCurrentItem();
				return true;
			
			case MENU_CLEARQUEUE:
				GetPlayerQueueTask clearQ = new GetPlayerQueueTask(this, this.playerReference, this, QueueAction.CLEAR);
				clearQ.execute();
				return true;
				
			case MENU_SHUFFLEQUEUE:
				GetPlayerQueueTask shuffleQ = new GetPlayerQueueTask(this, this.playerReference, this, QueueAction.SHUFFLE);
				shuffleQ.execute();
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.
	
	protected void refresh () {
		SetPlaystateTask playpauseTask = new SetPlaystateTask(this, this.playerReference, this);
		playpauseTask.execute();
		GetPlayerQueueTask queueTask = new GetPlayerQueueTask(this, this.playerReference, this);
		queueTask.execute();
	}
	
	protected void playpause () {
		SetPlaystateTask playpauseTask = new SetPlaystateTask(this, this.playerReference, TargetPlayState.PLAYPAUSE, this);
		playpauseTask.execute();
	}
	
	protected void next () {
		SetPlaystateTask playpauseTask = new SetPlaystateTask(this, this.playerReference, TargetPlayState.NEXT, this);
		playpauseTask.execute();
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
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.setItems(labels, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int item) {
					dialog.dismiss();
					SetPlaystateTask playpauseTask = new SetPlaystateTask(PlayerActivity.this, PlayerActivity.this.playerReference, item, PlayerActivity.this);
					playpauseTask.execute();
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
			DownloadMediaTask task = new DownloadMediaTask(this, this.mlistReference);
			task.execute(item);
		}
		else {
			Toast.makeText(this, "No item to download desu~", Toast.LENGTH_SHORT).show();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	UI updating.
	
	@Override
	public void onPlayerStateChange(PlayerState newState) {
		this.currentState = newState;
		
		if (newState == null) {
			finish(); // TODO show a msg here? Retry / Fail dlg?
		}
		else {
			if (newState.getListUrl() != null) {
				this.mlistReference = new MlistReferenceImpl(newState.getListUrl(), this.serverReference);
			}
			else {
				this.mlistReference = null;
			}
			
			TextView txtListname = (TextView) findViewById(R.id.txtListname);
			txtListname.setText(newState.getListTitle());
			
			TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
			if (newState.getTrackDuration() > 0) {
				txtTitle.setText(newState.getTrackTitle() + " (" + TimeHelper.formatTimeSeconds(newState.getTrackDuration()) + ")");
			}
			else {
				txtTitle.setText(newState.getTrackTitle());
			}
			
			TextView txtQueue = (TextView) findViewById(R.id.txtQueue);
			if (newState.getQueueDuration() > 0) {
				txtQueue.setText(newState.getQueueLength() + " items, "+TimeHelper.formatTimeSeconds(newState.getQueueDuration())+".");
			}
			else {
				txtQueue.setText(newState.getQueueLength() + " items.");
			}
			
			ImageView imgPlaystate = (ImageView) findViewById(R.id.imgPlaystate);
			imgPlaystate.setImageResource(newState.getImageResource());
		}
	}
	
	@Override
	public void onPlayerQueueChange(PlayerQueue newQueue) {
		if (newQueue == null) {
			finish(); // TODO show a msg here? Retry / Fail dlg?
		}
		else {
    		this.queueListAdaptor.setInputData(newQueue);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
