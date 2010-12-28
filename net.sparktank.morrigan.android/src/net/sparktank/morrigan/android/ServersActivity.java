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

import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.ServerReferenceListAdapter;
import net.sparktank.morrigan.android.model.impl.ServerReferenceImpl;
import net.sparktank.morrigan.android.model.impl.ServerReferenceListAdapterImpl;
import net.sparktank.morrigan.android.state.ConfigDb;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.ListView;
import android.widget.Toast;

public class ServersActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	ConfigDb configDb;
	ServerReferenceListAdapter serversListAdapter;
	
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
		this.serversListAdapter = new ServerReferenceListAdapterImpl(this);
		this.configDb = new ConfigDb(this);
		this.serversListAdapter.setInputData(this.configDb.getServers());
		
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
			ServerReference item = ServersActivity.this.serversListAdapter.getInputData().get(position);
			showServerActivity(item);
		}
	};
	
	private OnCreateContextMenuListener serversContextMenuListener = new OnCreateContextMenuListener () {
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			ServerReference item = ServersActivity.this.serversListAdapter.getInputData().get(info.position);
			menu.setHeaderTitle(item.getBaseUrl());
			menu.add(Menu.NONE, 1, Menu.NONE, "Remove");
		}
	};
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 1:
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
				ServerReference ref = ServersActivity.this.serversListAdapter.getInputData().get(info.position);
				deleteServer(ref);
				return true;
			
			default:
				return super.onContextItemSelected(item);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.
	
	protected void addServer () {
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
		dlgBuilder.setTitle("Add server");
		
		final EditText editText = new EditText(this);
		editText.setText("http://host:8080");
		dlgBuilder.setView(editText);
		
		dlgBuilder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String inputString = editText.getText().toString().trim();
				
				if (inputString == null || inputString.length() < 1) {
					dialog.cancel();
					return;
				}
				
				dialog.dismiss();
				// TODO validate inputString!
				
				// TODO find a tidier way to do this.
				ServersActivity.this.configDb.addServer(new ServerReferenceImpl(inputString));
				ServersActivity.this.serversListAdapter.setInputData(ServersActivity.this.configDb.getServers());
				ServersActivity.this.serversListAdapter.notifyDataSetChanged();
			}
		});
		
		dlgBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});
		
		dlgBuilder.show();
	}
	
	protected void deleteServer (ServerReference sr) {
		// TODO find a tidier way to do this.
		ServersActivity.this.configDb.removeServer(sr);
		ServersActivity.this.serversListAdapter.setInputData(ServersActivity.this.configDb.getServers());
		ServersActivity.this.serversListAdapter.notifyDataSetChanged();
		
		Toast.makeText(this, "Removed: " + sr.getBaseUrl(), Toast.LENGTH_SHORT).show();
	}
	
	protected void showServerActivity (ServerReference item) {
		Intent intent = new Intent(getApplicationContext(), ServerActivity.class);
		intent.putExtra(ServerActivity.BASE_URL, item.getBaseUrl()); // TODO pass a reference that can be used to get the ServerReference from the DB.
		startActivity(intent);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
