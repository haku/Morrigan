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

import net.sparktank.morrigan.android.model.PlayerState;
import net.sparktank.morrigan.android.model.PlayerStateChangeListener;
import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.impl.ServerReferenceImpl;
import net.sparktank.morrigan.android.tasks.SetPlaystateTask;
import net.sparktank.morrigan.android.tasks.SetPlaystateTask.TargetPlayState;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class PlayerActivity extends Activity implements PlayerStateChangeListener {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String BASE_URL = "baseUrl";
	public static final String PLAYER_ID = "playerId";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private ServerReference serverReference = null;
	private int playerId;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*
		 *  FIXME once ServerActivity is done, we should also receive a playerId here.
		 */
		
		Bundle extras = getIntent().getExtras();
		String baseUrl = extras.getString(BASE_URL);
		int id = extras.getInt(PLAYER_ID, -1);
		
		if (baseUrl != null) {
			this.serverReference = new ServerReferenceImpl(baseUrl); // TODO use data passed into activity to get ServerReference from DB.
		}
		else {
			finish();
		}
		
		if (id >= 0) {
			this.playerId = id;
		}
		else {
			finish();
		}
		
		this.setTitle(baseUrl + "/p" + this.playerId);
		
		setContentView(R.layout.player);
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
		
		cmd = (ImageButton) findViewById(R.id.btnPlaypause);
		cmd.setOnClickListener(new BtnPlaypause_OnClick());
		
		cmd = (ImageButton) findViewById(R.id.btnNext);
		cmd.setOnClickListener(new BtnNext_OnClick());
		
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
	
	class BtnRefresh_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			refresh();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Menu items.
	
	private static final int MENU_REFRESH = 1;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh).setIcon(R.drawable.ic_menu_refresh);
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			
			case MENU_REFRESH:
				refresh();
				return true;
			
		}
		
		return super.onOptionsItemSelected(item);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.
	
	protected void refresh () {
		SetPlaystateTask playpauseTask = new SetPlaystateTask(this, this.serverReference, null, this);
		playpauseTask.execute();
	}
	
	protected void playpause () {
		SetPlaystateTask playpauseTask = new SetPlaystateTask(this, this.serverReference, TargetPlayState.PLAYPAUSE, this);
		playpauseTask.execute();
	}
	
	protected void next () {
		SetPlaystateTask playpauseTask = new SetPlaystateTask(this, this.serverReference, TargetPlayState.NEXT, this);
		playpauseTask.execute();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	UI updating.
	
	@Override
	public void onPlayerStateChange(PlayerState newState) {
		if (newState == null) {
			finish(); // TODO show a msg here? Retry / Fail dlg?
		}
		else {
			TextView txtListname = (TextView) findViewById(R.id.txtListname);
			txtListname.setText(newState.getListTitle());
			
			TextView txtTitle = (TextView) findViewById(R.id.txtTitle);
			txtTitle.setText(newState.getTrackTitle());
			
			TextView txtQueue = (TextView) findViewById(R.id.txtQueue);
			txtQueue.setText(newState.getQueueLength() + " items.");
			
			ImageView imgPlaystate = (ImageView) findViewById(R.id.imgPlaystate);
			switch (newState.getPlayState()) {
				case STOPPED: imgPlaystate.setImageResource(R.drawable.stop);  break;
				case PLAYING: imgPlaystate.setImageResource(R.drawable.play);  break;
				case PAUSED:  imgPlaystate.setImageResource(R.drawable.pause); break;
				case LOADING: imgPlaystate.setImageResource(R.drawable.db);    break; // TODO find better icon.
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
