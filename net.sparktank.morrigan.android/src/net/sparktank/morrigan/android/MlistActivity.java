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
import net.sparktank.morrigan.android.model.MlistReference;
import net.sparktank.morrigan.android.model.MlistState;
import net.sparktank.morrigan.android.model.MlistStateChangeListener;
import net.sparktank.morrigan.android.model.impl.MlistReferenceImpl;
import net.sparktank.morrigan.android.tasks.GetMlistTask;
import net.sparktank.morrigan.android.tasks.RunMlistActionTask;
import net.sparktank.morrigan.android.tasks.RunMlistActionTask.CommandToRun;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class MlistActivity extends Activity implements MlistStateChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String BASE_URL = "baseUrl";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MlistReference mlistReference = null; 
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		String baseUrl = extras.getString(BASE_URL);
		
		if (baseUrl != null) {
			this.mlistReference = new MlistReferenceImpl(baseUrl);
		}
		else {
			finish();
		}
		
		this.setTitle(this.mlistReference.getBaseUrl());
		
		// TODO check return value.
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.mlist);
		hookUpButtons();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		refresh();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Buttons.
    
	private void hookUpButtons () {
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
		RunMlistActionTask task = new RunMlistActionTask(this, this.mlistReference, CommandToRun.PLAY);
		task.execute();
	}
	
	protected void queue () {
		RunMlistActionTask task = new RunMlistActionTask(this, this.mlistReference, CommandToRun.QUEUE);
		task.execute();
	}
	
	protected void scan () {
		RunMlistActionTask task = new RunMlistActionTask(this, this.mlistReference, CommandToRun.SCAN);
		task.execute();
	}
	
	protected void search () {
		Toast.makeText(this, "TODO: search", Toast.LENGTH_SHORT).show();
	}
	
	protected void refresh () {
		GetMlistTask task = new GetMlistTask(this, this.mlistReference, this);
		task.execute();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void onMlistStateChange(MlistState newState) {
		if (newState == null) {
			finish(); // TODO show a msg here? Retry / Fail dlg?
		}
		else {
			TextView txtListname = (TextView) findViewById(R.id.txtListname);
			txtListname.setText(newState.getTitle());
			
			TextView txtCount = (TextView) findViewById(R.id.txtCount);
			txtCount.setText(newState.getCount() + " items totalling "
					+ (newState.isDurationComplete() ? "" : "more than ")
					+ TimeHelper.formatTimeSeconds(newState.getDuration()) + ".");
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
