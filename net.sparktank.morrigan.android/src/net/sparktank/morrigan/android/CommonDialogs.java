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

import java.util.concurrent.atomic.AtomicReference;

import net.sparktank.morrigan.android.model.PlayerState;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.EditText;

public class CommonDialogs {
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
