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

package net.sparktank.morrigan.android;

import net.sparktank.morrigan.android.model.Artifact;
import net.sparktank.morrigan.android.model.ArtifactListAdaptor;
import net.sparktank.morrigan.android.model.MlistReference;
import net.sparktank.morrigan.android.model.MlistStateList;
import net.sparktank.morrigan.android.model.MlistStateListChangeListener;
import net.sparktank.morrigan.android.model.PlayerReference;
import net.sparktank.morrigan.android.model.PlayerStateList;
import net.sparktank.morrigan.android.model.PlayerStateListChangeListener;
import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.impl.ArtifactListAdaptorImpl;
import net.sparktank.morrigan.android.model.impl.ArtifactListGroupImpl;
import net.sparktank.morrigan.android.model.impl.ServerReferenceImpl;
import net.sparktank.morrigan.android.tasks.GetMlistsTask;
import net.sparktank.morrigan.android.tasks.GetPlayersTask;
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

public class ServerActivity extends Activity implements PlayerStateListChangeListener, MlistStateListChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String BASE_URL = "baseUrl";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	ServerReference serverReference = null;
	ArtifactListAdaptor artifactListAdaptor;
	private ArtifactListGroupImpl artifactListImpl;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		String baseUrl = extras.getString(BASE_URL);
		if (baseUrl != null) {
			this.serverReference = new ServerReferenceImpl(baseUrl); // TODO use data passed into activity to get ServerReference from DB.
		}
		else {
			finish();
		}
		this.setTitle(baseUrl);
		
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
		this.artifactListAdaptor = new ArtifactListAdaptorImpl(this);
		this.artifactListImpl = new ArtifactListGroupImpl();
		this.artifactListAdaptor.setInputData(this.artifactListImpl);
		
		ListView lstServers = (ListView) findViewById(R.id.lstServer);
		lstServers.setAdapter(this.artifactListAdaptor);
		lstServers.setOnItemClickListener(this.artifactsListCickListener);
		
		ImageButton cmd;
		
		cmd = (ImageButton) findViewById(R.id.btnRefresh);
		cmd.setOnClickListener(new BtnRefresh_OnClick());
		
		cmd = (ImageButton) findViewById(R.id.btnSearch);
		cmd.setOnClickListener(new BtnSearch_OnClick());
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
	
	class BtnSearch_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			search();
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
		intent.putExtra(PlayerActivity.BASE_URL, item.getBaseUrl());
		startActivity(intent);
	}
	
	protected void showArtifactActivity (MlistReference item) {
		Intent intent = new Intent(getApplicationContext(), MlistActivity.class);
		intent.putExtra(MlistActivity.BASE_URL, item.getBaseUrl());
		startActivity(intent);
	}
	
	protected void refresh () {
		GetPlayersTask playersTask = new GetPlayersTask(this, this.serverReference, this);
		playersTask.execute();
		GetMlistsTask mlistTask = new GetMlistsTask(this, this.serverReference, this);
		mlistTask.execute();
	}
	
	protected void search () {
		Toast.makeText(this, "TODO: search", Toast.LENGTH_SHORT).show();
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
