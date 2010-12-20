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
import android.widget.Toast;

public class PlayerActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	ServerReference serverReference = new ServerReferenceImpl(TempConstants.serverUrl);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);
		
		hookUpButtons();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Buttons.
    
	private void hookUpButtons () {
		ImageButton cmd;
		
		cmd = (ImageButton) findViewById(R.id.btnPlaypause);
		cmd.setOnClickListener(new BtnPlaypause_OnClick());
		
		cmd = (ImageButton) findViewById(R.id.btnNext);
		cmd.setOnClickListener(new BtnNext_OnClick());
	}
	
	class BtnPlaypause_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			SetPlaystateTask playpauseTask = new SetPlaystateTask(PlayerActivity.this, PlayerActivity.this.serverReference, TargetPlayState.PLAYPAUSE);
			playpauseTask.execute();
		}
	}
	
	class BtnNext_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			SetPlaystateTask playpauseTask = new SetPlaystateTask(PlayerActivity.this, PlayerActivity.this.serverReference, TargetPlayState.NEXT);
			playpauseTask.execute();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Menu items.
	
	private static final int MENU_REFRESH = 1;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh);
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			
			case MENU_REFRESH:
				Toast.makeText(getApplicationContext(), "TODO: refresh.", Toast.LENGTH_LONG).show();
				return true;
			
		}
		
		return super.onOptionsItemSelected(item);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
