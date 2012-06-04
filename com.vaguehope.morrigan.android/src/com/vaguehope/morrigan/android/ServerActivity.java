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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.vaguehope.morrigan.android.ServersList.ServerListEventsListener;
import com.vaguehope.morrigan.android.layouts.SidebarLayout;
import com.vaguehope.morrigan.android.layouts.SidebarLayout.SidebarListener;
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
import com.vaguehope.morrigan.android.modelimpl.ArtifactListAdaptorImpl;
import com.vaguehope.morrigan.android.modelimpl.ArtifactListGroupImpl;
import com.vaguehope.morrigan.android.state.ConfigDb;
import com.vaguehope.morrigan.android.tasks.GetMlistsTask;
import com.vaguehope.morrigan.android.tasks.GetPlayersTask;

public class ServerActivity extends Activity implements PlayerStateListChangeListener, MlistStateListChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String SERVER_ID = "serverId"; // int.

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	ConfigDb configDb;
	ServerReference serverReference = null;
	private SidebarLayout sidebarLayout;
	ArtifactListAdaptor<ArtifactList> artifactListAdaptor;
	private ArtifactListGroupImpl artifactListImpl;

	private ServersList serversList;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.configDb = new ConfigDb(this);
		wireGui();

		int serverId = (savedInstanceState == null) ? -1 : savedInstanceState.getInt(SERVER_ID, -1);
		if (serverId < 0) serverId = getIntent().getExtras().getInt(SERVER_ID, -1);
		if (serverId >= 0) {
			setServer(this.configDb.getServer(serverId));
		}
		else {
			finish(); // FIXME allow setServer(null);
		}
	}

	@Override
	protected void onStart () {
		super.onStart();
		refresh();
	}

	@Override
	protected void onSaveInstanceState (Bundle outState) {
		outState.putInt(SERVER_ID, this.serverReference.getId());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void setServer (ServerReference ref) {
		this.serverReference = ref;
		this.setTitle(this.serverReference.getName());
	}

	protected void refresh () {
		new GetPlayersTask(this, this.serverReference, this).execute();
		new GetMlistsTask(this, this.serverReference, this).execute();
	}

	public ServersList getServersList () {
		return this.serversList;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI methods.

	private void wireGui () {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // TODO check return value.
		setContentView(R.layout.server);

		this.sidebarLayout = (SidebarLayout) findViewById(R.id.serverLayout);
		this.sidebarLayout.setListener(this.sidebarListener);

		this.artifactListAdaptor = new ArtifactListAdaptorImpl<ArtifactList>(this, R.layout.simplelistrow);
		this.artifactListImpl = new ArtifactListGroupImpl();
		this.artifactListAdaptor.setInputData(this.artifactListImpl);

		ListView lstServers = (ListView) findViewById(R.id.lstServer);
		lstServers.setAdapter(this.artifactListAdaptor);
		lstServers.setOnItemClickListener(this.artifactsListCickListener);

		ImageButton btnRefresh = (ImageButton) findViewById(R.id.btnRefresh);
		btnRefresh.setOnClickListener(new BtnRefresh_OnClick());

		ImageButton btnSidebar = (ImageButton) findViewById(R.id.btnSidebar);
		btnSidebar.setOnClickListener(new SidebarLayout.ToggleSidebarListener(this.sidebarLayout));

		ListView lstSidebar = (ListView) findViewById(R.id.lstSidebar);
		this.serversList = new ServersList(this, lstSidebar, this.configDb, this.serverListEventsListener);

		ImageButton btnAddServer = (ImageButton) findViewById(R.id.btnAddServer);
		btnAddServer.setOnClickListener(this.btnAddServerClickListener);
	}

	private OnItemClickListener artifactsListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
			Artifact item = ServerActivity.this.artifactListAdaptor.getInputData().getArtifactList().get(position);
			showArtifactActivity(item);
		}
	};

	class BtnRefresh_OnClick implements OnClickListener {
		@Override
		public void onClick (View v) {
			refresh();
		}
	}

	ServerListEventsListener serverListEventsListener = new ServerListEventsListener() {
		@Override
		public void showServer (ServerReference ref) {
			setServer(ref);
			refresh();
		}
	};

	private OnClickListener btnAddServerClickListener = new OnClickListener () {
		@Override
		public void onClick(View v) {
			getServersList().promptAddServer();
		}
	};

	@Override
	public void onBackPressed () {
		if (!this.sidebarLayout.closeSidebar()) finish();
	}

	@Override
	public boolean onContextItemSelected (MenuItem item) {
		boolean handled = this.serversList.handleOnContextItemSelected(item);
		if (!handled) handled = super.onContextItemSelected(item);
		return handled;
	}

	private final SidebarListener sidebarListener = new SidebarListener() {

		@Override
		public boolean onContentTouchedWhenOpening (SidebarLayout sidebar) {
			sidebar.closeSidebar();
			return true;
		}

		@Override
		public void onSidebarOpened (SidebarLayout sidebar) {/* Unused. */}

		@Override
		public void onSidebarClosed (SidebarLayout sidebar) {/* Unused. */}
	};

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
			Toast.makeText(this, "TODO: open type '" + item.getClass().getName() + "' desu~", Toast.LENGTH_LONG).show();
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

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	UI updating.

	@Override
	public void onPlayersChange (PlayerStateList playersState) {
		if (playersState == null) {
			finish(); // FIXME show a msg here? Retry / Fail dlg?
		}
		else {
			this.artifactListImpl.addList("players", playersState);
			this.artifactListAdaptor.notifyDataSetChanged();
		}
	}

	@Override
	public void onMlistsChange (MlistStateList mlistsState) {
		if (mlistsState == null) {
			finish(); // FIXME show a msg here? Retry / Fail dlg?
		}
		else {
			this.artifactListImpl.addList("mlists", mlistsState);
			this.artifactListAdaptor.notifyDataSetChanged();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
