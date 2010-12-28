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

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.ServerReferenceListAdapter;
import net.sparktank.morrigan.android.model.impl.ServerReferenceImpl;
import net.sparktank.morrigan.android.model.impl.ServerReferenceListAdapterImpl;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

public class ServersActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
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
		
		// FIXME temp test data.
		List<ServerReference> data = new ArrayList<ServerReference>();
		data.add(new ServerReferenceImpl(TempConstants.serverUrl));
		this.serversListAdapter.setInputData(data);
		
		ListView lstServers = (ListView) findViewById(R.id.lstServers);
		lstServers.setAdapter(this.serversListAdapter);
		lstServers.setOnItemClickListener(this.serversListCickListener);
		
		ImageButton cmd;
		cmd = (ImageButton) findViewById(R.id.btnAddServer);
		cmd.setOnClickListener(new BtnAddServer_OnClick());
	}
	
	private OnItemClickListener serversListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			ServerReference item = ServersActivity.this.serversListAdapter.getInputData().get(position);
			showServerActivity(item);
		}
	};
	
	private class BtnAddServer_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			addServer();
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
				
				serversListAdapter.getInputData().add(new ServerReferenceImpl(inputString));
				serversListAdapter.notifyDataSetChanged();
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
	
	protected void showServerActivity (ServerReference item) {
		Intent intent = new Intent(getApplicationContext(), ServerActivity.class);
		intent.putExtra(ServerActivity.BASE_URL, item.getBaseUrl()); // TODO pass a reference that can be used to get the ServerReference from the DB.
		startActivity(intent);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
