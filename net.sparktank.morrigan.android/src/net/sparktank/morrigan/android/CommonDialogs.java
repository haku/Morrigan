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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.sparktank.morrigan.android.model.PlayerState;
import net.sparktank.morrigan.android.model.PlayerStateList;
import net.sparktank.morrigan.android.model.PlayerStateListChangeListener;
import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.tasks.GetPlayersTask;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

public class CommonDialogs {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public interface PlayerSelectedListener {
		public void playerSelected (PlayerState playerState);
	}
	
	static public void doAskWhichPlayer (final Context context, final ServerReference serverReference, final PlayerSelectedListener listener) {
		GetPlayersTask task = new GetPlayersTask(context, serverReference, new PlayerStateListChangeListener () {
			@Override
			public void onPlayersChange(PlayerStateList playersState) {
				List<? extends PlayerState> playerList = playersState.getPlayersStateList();
				
				if (playerList == null || playerList.size() < 1) {
					Toast.makeText(context, "No players found.", Toast.LENGTH_LONG).show();
					return;
				}
				
				if (playerList.size() == 1) {
					listener.playerSelected(playerList.iterator().next());
					return;
				}
				
				final List<? extends PlayerState> list = playerList;
				String[] labels = new String[playerList.size()];
				for (int i = 0; i < playerList.size(); i ++) {
					PlayerState ps = playerList.get(i);
					labels[i] = "Player " + ps.getId() + " (" + ps.getPlayState() + ")";
				}
				
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Select player");
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.setItems(labels, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int item) {
						dialog.dismiss();
						listener.playerSelected(list.get(item));
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
		});
		task.execute();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void doSearchMlist (final Context context, final PlayerState playerState) {
		doSearchMlist(context, playerState, null);
	}
	
	static public void doSearchMlist (final Context context, final PlayerState playerState, final AtomicReference<String> defaultQuery) {
		if (context == null) throw new IllegalArgumentException();
		if (playerState == null) throw new IllegalArgumentException();
		
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(context);
		dlgBuilder.setTitle("Query " + playerState.getListTitle());
		
		final EditText editText = new EditText(context);
		if (defaultQuery != null && defaultQuery.get() != null) editText.setText(defaultQuery.get());
		dlgBuilder.setView(editText);
		
		dlgBuilder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				String query = editText.getText().toString().trim();
				dialog.dismiss();
				
				if (defaultQuery != null) defaultQuery.set(query);
				
				Intent intent = new Intent(context.getApplicationContext(), MlistActivity.class);
				intent.putExtra(MlistActivity.SERVER_BASE_URL, playerState.getPlayerReference().getServerReference().getBaseUrl());
				intent.putExtra(MlistActivity.MLIST_BASE_URL, playerState.getListUrl());
				intent.putExtra(MlistActivity.QUERY, query);
				intent.putExtra(MlistActivity.PLAYER_ID, playerState.getPlayerReference().getPlayerId());
				context.startActivity(intent);
				
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
}
