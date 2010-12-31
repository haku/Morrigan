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

import net.sparktank.morrigan.android.helper.TimeHelper;
import net.sparktank.morrigan.android.model.MlistItem;
import net.sparktank.morrigan.android.model.MlistItemList;
import net.sparktank.morrigan.android.model.MlistItemListChangeListener;
import net.sparktank.morrigan.android.model.MlistReference;
import net.sparktank.morrigan.android.model.MlistState;
import net.sparktank.morrigan.android.model.MlistStateChangeListener;
import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.impl.ArtifactListAdaptorImpl;
import net.sparktank.morrigan.android.model.impl.MlistReferenceImpl;
import net.sparktank.morrigan.android.model.impl.ServerReferenceImpl;
import net.sparktank.morrigan.android.tasks.GetMlistItemListTask;
import net.sparktank.morrigan.android.tasks.GetMlistTask;
import net.sparktank.morrigan.android.tasks.RunMlistActionTask;
import net.sparktank.morrigan.android.tasks.RunMlistActionTask.MlistCommand;
import net.sparktank.morrigan.android.tasks.RunMlistItemActionTask;
import net.sparktank.morrigan.android.tasks.RunMlistItemActionTask.MlistItemCommand;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class MlistActivity extends Activity implements MlistStateChangeListener, MlistItemListChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String SERVER_BASE_URL = "serverBaseUrl";
	public static final String MLIST_BASE_URL = "mlistBaseUrl";
	
	public static final String QUERY = "query";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected ServerReference serverReference = null;
	protected MlistReference mlistReference = null;
	protected ArtifactListAdaptorImpl<MlistItemList> mlistItemListAdapter;
	private MlistState currentState = null;
	private MlistItemList currentItemList = null;
	private String initialQuery = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		String serverBaseUrl = extras.getString(SERVER_BASE_URL);
		String mlistBaseUrl = extras.getString(MLIST_BASE_URL);
		this.initialQuery = extras.getString(QUERY);
		
		if (serverBaseUrl != null && mlistBaseUrl != null) {
			this.serverReference = new ServerReferenceImpl(serverBaseUrl);
			this.mlistReference = new MlistReferenceImpl(mlistBaseUrl, this.serverReference);
		}
		else {
			finish();
		}
		
		this.setTitle(this.mlistReference.getBaseUrl());
		
		// TODO check return value.
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.mlist);
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
		this.mlistItemListAdapter = new ArtifactListAdaptorImpl<MlistItemList>(this, R.layout.mlistitemlistrow);
		
		ListView lstServers = (ListView) findViewById(R.id.lstItems);
		lstServers.setAdapter(this.mlistItemListAdapter);
		lstServers.setOnItemClickListener(this.mlistItemListCickListener);
		
		ImageButton cmd;
		
		cmd = (ImageButton) findViewById(R.id.btnPlay);
		cmd.setOnClickListener(new BtnPlay_OnClick());
		
		cmd = (ImageButton) findViewById(R.id.btnQueue);
		cmd.setOnClickListener(new BtnQueue_OnClick());
		
		cmd = (ImageButton) findViewById(R.id.btnSearch);
		cmd.setOnClickListener(new BtnSearch_OnClick());
		
		cmd = (ImageButton) findViewById(R.id.btnRefresh);
		cmd.setOnClickListener(new BtnRefresh_OnClick());
		
	}
	
	private OnItemClickListener mlistItemListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			MlistItem item = MlistActivity.this.mlistItemListAdapter.getInputData().getMlistItemList().get(position);
			itemClicked(item);
		}
	};
	
	class BtnPlay_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			play();
		}
	}
	
	class BtnQueue_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			queue();
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
	
	private static final int MENU_SCAN = 1;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_SCAN, 0, R.string.menu_scan);
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			
			case MENU_SCAN:
				scan();
				return true;
			
		}
		
		return super.onOptionsItemSelected(item);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.
	
	protected void play () {
		RunMlistActionTask task = new RunMlistActionTask(this, this.mlistReference, MlistCommand.PLAY);
		task.execute();
	}
	
	protected void queue () {
		RunMlistActionTask task = new RunMlistActionTask(this, this.mlistReference, MlistCommand.QUEUE);
		task.execute();
	}
	
	protected void scan () {
		RunMlistActionTask task = new RunMlistActionTask(this, this.mlistReference, MlistCommand.SCAN);
		task.execute();
	}
	
	protected void search () {
		if (this.currentState == null) return; // TODO show msg here?
		
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
		dlgBuilder.setTitle("Query " + this.currentState.getTitle());
		
		final EditText editText = new EditText(this);
		if (this.currentItemList != null) editText.setText(this.currentItemList.getQuery());
		dlgBuilder.setView(editText);
		
		dlgBuilder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String query = editText.getText().toString().trim();
				dialog.dismiss();
				
				GetMlistItemListTask task = new GetMlistItemListTask(MlistActivity.this, MlistActivity.this.mlistReference, MlistActivity.this, query);
				task.execute();
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
	
	protected void refresh () {
		GetMlistTask task = new GetMlistTask(this, this.mlistReference, this);
		task.execute();
		
		String query = null;
		if (this.currentItemList != null) {
			query = this.currentItemList.getQuery();
		}
		else if (this.initialQuery != null) {
			query = this.initialQuery;
			this.initialQuery = null;
		}
		
		if (query != null) {
    		GetMlistItemListTask task2 = new GetMlistItemListTask(
    				MlistActivity.this, MlistActivity.this.mlistReference,
    				MlistActivity.this, query);
    		task2.execute();
		}
	}
	
	protected void itemClicked (final MlistItem item) {
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
		dlgBuilder.setMessage(item.getTitle());
		
		dlgBuilder.setPositiveButton("Play", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				RunMlistItemActionTask task = new RunMlistItemActionTask(MlistActivity.this, MlistActivity.this.serverReference, item, MlistItemCommand.PLAY);
				task.execute();
			}
		});
		
		dlgBuilder.setNeutralButton("Queue", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				RunMlistItemActionTask task = new RunMlistItemActionTask(MlistActivity.this, MlistActivity.this.serverReference, item, MlistItemCommand.QUEUE);
				task.execute();
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
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void onMlistStateChange(MlistState newState) {
		this.currentState = newState;
		
		if (newState == null) {
			finish(); // TODO show a msg here? Retry / Fail dlg?
		}
		else {
			this.setTitle(newState.getTitle());
			
			TextView txtCount = (TextView) findViewById(R.id.txtTitle);
			txtCount.setText(newState.getCount() + " items, "
					+ (newState.isDurationComplete() ? "" : "> ")
					+ TimeHelper.formatTimeSeconds(newState.getDuration()) + ".");
			
		}
	}
	
	@Override
	public void onMlistItemListChange(MlistItemList mlistItemList) {
		this.currentItemList = mlistItemList;
		
		if (mlistItemList == null) {
			finish(); // TODO show a msg here? Retry / Fail dlg?
		}
		else {
    		TextView txtSubTitle = (TextView) findViewById(R.id.txtSubTitle);
    		txtSubTitle.setText(mlistItemList.getMlistItemList().size() + " results for '"+mlistItemList.getQuery()+"'.");
    		
    		this.mlistItemListAdapter.setInputData(mlistItemList);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
