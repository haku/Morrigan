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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.vaguehope.morrigan.android.model.Artifact;
import com.vaguehope.morrigan.android.model.ArtifactList;
import com.vaguehope.morrigan.android.model.ArtifactListAdaptor;
import com.vaguehope.morrigan.android.model.MlistReference;
import com.vaguehope.morrigan.android.model.MlistStateList;
import com.vaguehope.morrigan.android.model.MlistStateListChangeListener;
import com.vaguehope.morrigan.android.model.PlayerReference;
import com.vaguehope.morrigan.android.model.PlayerStateList;
import com.vaguehope.morrigan.android.model.PlayerStateListChangeListener;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.model.impl.ArtifactListAdaptorImpl;
import com.vaguehope.morrigan.android.model.impl.ArtifactListGroupImpl;
import com.vaguehope.morrigan.android.state.ConfigDb;
import com.vaguehope.morrigan.android.tasks.GetMlistsTask;
import com.vaguehope.morrigan.android.tasks.GetPlayersTask;

public class ServerActivity extends Activity implements PlayerStateListChangeListener, MlistStateListChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String SERVER_ID = "serverId"; // int.
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	ConfigDb configDb;
	ServerReference serverReference = null;
	ArtifactListAdaptor<ArtifactList> artifactListAdaptor;
	private ArtifactListGroupImpl artifactListImpl;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.configDb = new ConfigDb(this);
		
		Bundle extras = getIntent().getExtras();
		int serverId = extras.getInt(SERVER_ID, -1);
		if (serverId >= 0) {
			this.serverReference = this.configDb.getServer(serverId);
		}
		else {
			finish();
		}
		this.setTitle(this.serverReference.getName());
		
		// TODO check return value.
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.server);
		wireGui();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		refresh();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI methods.
	
	private void wireGui () {
		this.artifactListAdaptor = new ArtifactListAdaptorImpl<ArtifactList>(this, R.layout.simplelistrow);
		this.artifactListImpl = new ArtifactListGroupImpl();
		this.artifactListAdaptor.setInputData(this.artifactListImpl);
		
		ListView lstServers = (ListView) findViewById(R.id.lstServer);
		lstServers.setAdapter(this.artifactListAdaptor);
		lstServers.setOnItemClickListener(this.artifactsListCickListener);
		
		ImageButton cmd;
		
		cmd = (ImageButton) findViewById(R.id.btnRefresh);
		cmd.setOnClickListener(new BtnRefresh_OnClick());
	}
	
	private OnItemClickListener artifactsListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Artifact item = ServerActivity.this.artifactListAdaptor.getInputData().getArtifactList().get(position);
			showArtifactActivity(item);
		}
	};
	
	class BtnRefresh_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			refresh();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.
	
	protected void showArtifactActivity (Artifact item) {
		if (item instanceof PlayerReference) {
			PlayerReference pr = (PlayerReference) item;
			showArtifactActivity(pr);
		}
		else if (item instanceof MlistReference) {
			MlistReference mr = (MlistReference) item;
			showArtifactActivity(mr);
		}
		else {
			Toast.makeText(this, "TODO: open type '"+item.getClass().getName()+"' desu~", Toast.LENGTH_LONG).show();
		}
	}
	
	protected void showArtifactActivity (PlayerReference item) {
		Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
		intent.putExtra(MlistActivity.SERVER_ID, this.serverReference.getId());
		intent.putExtra(PlayerActivity.PLAYER_ID, item.getPlayerId());
		startActivity(intent);
	}
	
	protected void showArtifactActivity (MlistReference item) {
		Intent intent = new Intent(getApplicationContext(), MlistActivity.class);
		intent.putExtra(MlistActivity.SERVER_ID, this.serverReference.getId());
		intent.putExtra(MlistActivity.MLIST_BASE_URL, item.getBaseUrl());
		intent.putExtra(MlistActivity.QUERY, "*"); // Default to showing results of wild-card search.
		startActivity(intent);
	}
	
	protected void refresh () {
		GetPlayersTask playersTask = new GetPlayersTask(this, this.serverReference, this);
		playersTask.execute();
		GetMlistsTask mlistTask = new GetMlistsTask(this, this.serverReference, this);
		mlistTask.execute();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	UI updating.
	
	@Override
	public void onPlayersChange(PlayerStateList playersState) {
		if (playersState == null) {
			finish(); // TODO show a msg here? Retry / Fail dlg?
		}
		else {
			this.artifactListImpl.addList("players", playersState);
			this.artifactListAdaptor.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onMlistsChange(MlistStateList mlistsState) {
		if (mlistsState == null) {
			finish(); // TODO show a msg here? Retry / Fail dlg?
		}
		else {
			this.artifactListImpl.addList("mlists", mlistsState);
			this.artifactListAdaptor.notifyDataSetChanged();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
