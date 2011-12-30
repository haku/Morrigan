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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.vaguehope.morrigan.android.model.ArtifactListAdaptor;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.model.ServerReferenceList;
import com.vaguehope.morrigan.android.modelimpl.ArtifactListAdaptorImpl;
import com.vaguehope.morrigan.android.modelimpl.ServerReferenceImpl;
import com.vaguehope.morrigan.android.state.ConfigDb;

public class ServersActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final int MENU_EDIT = 1;
	private static final int MENU_DELETE = 2;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	ConfigDb configDb;
	ArtifactListAdaptor<ServerReferenceList> serversListAdapter;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.servers);
		
		wireGui();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI methods.
	
	private void wireGui () {
		this.serversListAdapter = new ArtifactListAdaptorImpl<ServerReferenceList>(this, R.layout.simplelistrow);
		this.configDb = new ConfigDb(this);
		this.serversListAdapter.setInputData(this.configDb);
		
		ListView lstServers = (ListView) findViewById(R.id.lstServers);
		lstServers.setAdapter(this.serversListAdapter);
		lstServers.setOnItemClickListener(this.serversListCickListener);
		lstServers.setOnCreateContextMenuListener(this.serversContextMenuListener);
		
		ImageButton cmd;
		cmd = (ImageButton) findViewById(R.id.btnAddServer);
		cmd.setOnClickListener(this.btnAddServerClickListener);
	}
	
	private OnClickListener btnAddServerClickListener = new OnClickListener () {
		@Override
		public void onClick(View v) {
			addServer();
		}
	};
	
	private OnItemClickListener serversListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			ServerReference item = ServersActivity.this.serversListAdapter.getInputData().getServerReferenceList().get(position);
			showServerActivity(item);
		}
	};
	
	private OnCreateContextMenuListener serversContextMenuListener = new OnCreateContextMenuListener () {
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			ServerReference item = ServersActivity.this.serversListAdapter.getInputData().getServerReferenceList().get(info.position);
			menu.setHeaderTitle(item.getBaseUrl());
			menu.add(Menu.NONE, MENU_EDIT, Menu.NONE, "Edit");
			menu.add(Menu.NONE, MENU_DELETE, Menu.NONE, "Remove");
		}
	};
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		ServerReference ref;
		
		switch (item.getItemId()) {
			case MENU_EDIT:
				info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
				ref = ServersActivity.this.serversListAdapter.getInputData().getServerReferenceList().get(info.position);
				editServer(ref);
				return true;
				
			case MENU_DELETE:
				info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
				ref = ServersActivity.this.serversListAdapter.getInputData().getServerReferenceList().get(info.position);
				deleteServer(ref);
				return true;
			
			default:
				return super.onContextItemSelected(item);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.
	
	protected void addServer () {
		final ServerDlg dlg = new ServerDlg(this);
		dlg.getBldr().setPositiveButton("Add", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				if (!dlg.isSet()) {
					dialog.cancel();
					return;
				}
				dialog.dismiss();
				
				ServersActivity.this.configDb.addServer(new ServerReferenceImpl(dlg.getName(), dlg.getUrl(), dlg.getPass()));
				ServersActivity.this.serversListAdapter.notifyDataSetChanged();
			}
		});
		dlg.show();
	}
	
	protected void editServer (final ServerReference ref) {
		final ServerDlg dlg = new ServerDlg(this, ref);
		dlg.getBldr().setPositiveButton("Update", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				if (!dlg.isSet()) {
					dialog.cancel();
					return;
				}
				dialog.dismiss();
				
				ServersActivity.this.configDb.updateServer(new ServerReferenceImpl(ref.getId(), dlg.getName(), dlg.getUrl(), dlg.getPass()));
				ServersActivity.this.serversListAdapter.notifyDataSetChanged();
			}
		});
		dlg.show();
	}
	protected void deleteServer (final ServerReference sr) {
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
		dlgBuilder.setMessage("Delete: " + sr.getTitle());
		
		dlgBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (DialogInterface dialog, int which) {
				ServersActivity.this.configDb.removeServer(sr);
				ServersActivity.this.serversListAdapter.notifyDataSetChanged();
				Toast.makeText(ServersActivity.this, "Removed: " + sr.getBaseUrl(), Toast.LENGTH_SHORT).show();
			}
		});
		
		dlgBuilder.setNegativeButton("Keep", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});
		
		dlgBuilder.show();
	}
	
	static class ServerDlg {
		
		private final AlertDialog.Builder bldr;
		private final EditText txtName;
		private final EditText txtUrl;
		private final EditText txtPass;
		
		public ServerDlg (Context context) {
			this(context, null);
		}
		
		public ServerDlg (Context context, ServerReference ref) {
			this.bldr = new AlertDialog.Builder(context);
			this.bldr.setTitle("Server");
			
			this.txtName = new EditText(context);
			this.txtName.setHint("name");
			if (ref != null) this.txtName.setText(ref.getName());
			
			this.txtUrl = new EditText(context);
			this.txtUrl.setHint("url");
			this.txtUrl.setText(ref != null ? ref.getBaseUrl() : "http://host:8080");
			
			this.txtPass = new EditText(context);
			this.txtPass.setHint("pass");
			if (ref != null) this.txtPass.setText(ref.getPass());
			
			LinearLayout layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.addView(this.txtName);
			layout.addView(this.txtUrl);
			layout.addView(this.txtPass);
			this.bldr.setView(layout);
			
			this.bldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			});
		}
		
		public AlertDialog.Builder getBldr () {
			return this.bldr;
		}
		
		public boolean isSet () {
			String name = getName();
			String url = getUrl();
			String pass = getPass();
			return (name != null && name.length() > 0
					&& url != null && url.length() > 0
					&& pass != null && pass.length() > 0
					);
		}
		
		public String getName () {
			return this.txtName.getText().toString().trim();
		}
		
		public String getUrl () {
			return this.txtUrl.getText().toString().trim();
		}
		
		public String getPass () {
			return this.txtPass.getText().toString().trim();
		}
		
		public void show () {
			this.bldr.show();
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected void showServerActivity (ServerReference item) {
		Intent intent = new Intent(getApplicationContext(), ServerActivity.class);
		intent.putExtra(ServerActivity.SERVER_ID, item.getId());
		startActivity(intent);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
