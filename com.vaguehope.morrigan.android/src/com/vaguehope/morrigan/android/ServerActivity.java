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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
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
import com.vaguehope.morrigan.android.checkout.CheckoutMgrActivity;
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
import com.vaguehope.morrigan.android.playback.PlaybackActivity;
import com.vaguehope.morrigan.android.state.ConfigDb;
import com.vaguehope.morrigan.android.state.Preferences;
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
	private ErrorsList errorsList;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.configDb = new ConfigDb(this);
		wireGui();

		String serverId = (savedInstanceState == null) ? null : savedInstanceState.getString(SERVER_ID);
		if (serverId == null) {
			Bundle extras = getIntent().getExtras();
			if (extras != null) serverId = extras.getString(SERVER_ID);
		}
		if (serverId == null) {
			serverId = Preferences.getCurrentServer(this);
		}
		if (serverId != null) {
			setServer(this.configDb.getServer(serverId));
		}
		else {
			setServer(null);
			this.sidebarLayout.openSidebar();
		}
	}

	@Override
	protected void onStart () {
		super.onStart();
		refresh();
	}

	@Override
	protected void onSaveInstanceState (final Bundle outState) {
		if (this.serverReference != null) outState.putString(SERVER_ID, this.serverReference.getId());
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause () {
		if (this.serverReference != null) Preferences.putCurrentServer(this, this.serverReference.getId());
		super.onPause();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected void setServer (final ServerReference ref) {
		this.serverReference = ref;
		this.setTitle((this.serverReference == null) ? "Morrigan" : this.serverReference.getName());
	}

	protected void refresh () {
		if (this.serverReference != null) {
			new GetPlayersTask(this, this.serverReference, this).execute();
			new GetMlistsTask(this, this.serverReference, this).execute();
		}
		else {
			onPlayersChange(null, null);
			onMlistsChange(null, null);
			this.setProgressBarIndeterminateVisibility(false);
		}
	}

	public ServersList getServersList () {
		return this.serversList;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI methods.

	private void wireGui () {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // TODO check return value.
		setContentView(R.layout.server);

		final ActionBar ab = getActionBar();
		ab.setDisplayShowHomeEnabled(true);
		ab.setHomeButtonEnabled(true);

		this.sidebarLayout = (SidebarLayout) findViewById(R.id.serverLayout);
		this.sidebarLayout.setListener(this.sidebarListener);

		ListView lstErrors = (ListView) findViewById(R.id.lstErrors);
		this.errorsList = new ErrorsList(this, lstErrors);

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

	private final OnItemClickListener artifactsListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			Artifact item = ServerActivity.this.artifactListAdaptor.getInputData().getArtifactList().get(position);
			showArtifactActivity(item);
		}
	};

	class BtnRefresh_OnClick implements OnClickListener {
		@Override
		public void onClick (final View v) {
			refresh();
		}
	}

	ServerListEventsListener serverListEventsListener = new ServerListEventsListener() {
		@Override
		public void showServer (final ServerReference ref) {
			setServer(ref);
			refresh();
		}
	};

	private final OnClickListener btnAddServerClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			getServersList().promptAddServer();
		}
	};

	@Override
	public void onBackPressed () {
		if (!this.sidebarLayout.closeSidebar()) finish();
	}

	@Override
	public boolean onContextItemSelected (final MenuItem item) {
		boolean handled = this.serversList.handleOnContextItemSelected(item);
		if (!handled) handled = super.onContextItemSelected(item);
		return handled;
	}

	@Override
	public boolean onCreateOptionsMenu (final Menu menu) {
		getMenuInflater().inflate(R.menu.servermenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.sidebarLayout.toggleSidebar();
				return true;
			case R.id.playback:
				startActivity(new Intent(getApplicationContext(), PlaybackActivity.class)
						.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
				return true;
			case R.id.checkoutmgr:
				startActivity(new Intent(getApplicationContext(), CheckoutMgrActivity.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private final SidebarListener sidebarListener = new SidebarListener() {

		@Override
		public boolean onContentTouchedWhenOpening (final SidebarLayout sidebar) {
			sidebar.closeSidebar();
			return true;
		}

		@Override
		public void onSidebarOpened (final SidebarLayout sidebar) {/* Unused. */}

		@Override
		public void onSidebarClosed (final SidebarLayout sidebar) {/* Unused. */}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.

	protected void showArtifactActivity (final Artifact item) {
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

	protected void showArtifactActivity (final PlayerReference item) {
		Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
		intent.putExtra(MlistActivity.SERVER_ID, this.serverReference.getId());
		intent.putExtra(PlayerActivity.PLAYER_ID, item.getPlayerId());
		startActivity(intent);
	}

	protected void showArtifactActivity (final MlistReference item) {
		Intent intent = new Intent(getApplicationContext(), MlistActivity.class);
		intent.putExtra(MlistActivity.SERVER_ID, this.serverReference.getId());
		intent.putExtra(MlistActivity.MLIST_BASE_URL, item.getBaseUrl());
		intent.putExtra(MlistActivity.QUERY, "*"); // Default to showing results of wild-card search.
		startActivity(intent);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	UI updating.

	private static final String LIST_PLAYERS = "players";
	private static final String LIST_MLISTS = "mlists";

	@Override
	public void onPlayersChange (final PlayerStateList playersState, final Exception e) {
		this.artifactListImpl.addList(LIST_PLAYERS, playersState);
		this.artifactListAdaptor.notifyDataSetChanged();
		this.errorsList.setError(LIST_PLAYERS, e);
	}

	@Override
	public void onMlistsChange (final MlistStateList mlistsState, final Exception e) {
		this.artifactListImpl.addList(LIST_MLISTS, mlistsState);
		this.artifactListAdaptor.notifyDataSetChanged();
		this.errorsList.setError(LIST_MLISTS, e);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
