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

package net.sparktank.morrigan.android;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import net.sparktank.morrigan.android.helper.TimeHelper;
import net.sparktank.morrigan.android.model.ArtifactList;
import net.sparktank.morrigan.android.model.ArtifactListAdaptor;
import net.sparktank.morrigan.android.model.PlayerQueue;
import net.sparktank.morrigan.android.model.PlayerQueueChangeListener;
import net.sparktank.morrigan.android.model.PlayerReference;
import net.sparktank.morrigan.android.model.PlayerState;
import net.sparktank.morrigan.android.model.PlayerStateChangeListener;
import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.impl.ArtifactListAdaptorImpl;
import net.sparktank.morrigan.android.model.impl.PlayerReferenceImpl;
import net.sparktank.morrigan.android.model.impl.ServerReferenceImpl;
import net.sparktank.morrigan.android.tasks.GetPlayerQueueTask;
import net.sparktank.morrigan.android.tasks.SetPlaystateTask;
import net.sparktank.morrigan.android.tasks.SetPlaystateTask.TargetPlayState;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerActivity extends Activity implements PlayerStateChangeListener, PlayerQueueChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String SERVER_BASE_URL = "serverBaseUrl"; // String.
	public static final String PLAYER_ID = "playerId"; // int.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected ServerReference serverReference = null;
	protected PlayerReference playerReference = null;
	private PlayerState currentState;
	private ArtifactListAdaptor<ArtifactList> queueListAdaptor;
	
	private AtomicReference<String> lastQuery = new AtomicReference<String>();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		String serverBaseUrl = extras.getString(SERVER_BASE_URL);
		int playerId = extras.getInt(PLAYER_ID, -1);
		
		if (serverBaseUrl != null && playerId >= 0) {
			this.serverReference = new ServerReferenceImpl(serverBaseUrl);
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
		this.queueListAdaptor = new ArtifactListAdaptorImpl<ArtifactList>(this, R.layout.mlistitemlistrow);
		ListView lstQueue = (ListView) findViewById(R.id.lstQueue);
		lstQueue.setAdapter(this.queueListAdaptor);
		
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
//	Menu items.
	
	private static final int MENU_FULLSCREEN = 2;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_FULLSCREEN, 0, R.string.menu_fullscreen).setIcon(R.drawable.display);
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			
			case MENU_FULLSCREEN:
				fullscreen();
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	UI updating.
	
	@Override
	public void onPlayerStateChange(PlayerState newState) {
		this.currentState = newState;
		
		if (newState == null) {
			finish(); // TODO show a msg here? Retry / Fail dlg?
		}
		else {
			TextView txtListname = (TextView) findViewById(R.id.txtListname);
			txtListname.setText(newState.getListTitle());
			
			TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
			txtTitle.setText(newState.getTrackTitle() + " (" + TimeHelper.formatTimeSeconds(newState.getTrackDuration()) + ")");
			
			TextView txtQueue = (TextView) findViewById(R.id.txtQueue);
			txtQueue.setText(newState.getQueueLength() + " items.");
			
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
