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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.MlistReference;
import com.vaguehope.morrigan.android.model.PlayerState;
import com.vaguehope.morrigan.android.model.PlayerStateList;
import com.vaguehope.morrigan.android.model.PlayerStateListChangeListener;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.tasks.GetPlayersTask;
import com.vaguehope.morrigan.android.tasks.RunMlistItemActionTask;

public class CommonDialogs {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static interface PlayerSelectedListener {
		public void playerSelected (PlayerState playerState);
	}

	public static void doAskWhichPlayer (final Context context, final ServerReference serverReference, final PlayerSelectedListener listener) {
		GetPlayersTask task = new GetPlayersTask(context, serverReference, new PlayerStateListChangeListener() {
			@Override
			public void onPlayersChange (PlayerStateList playersState, Exception e) {
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
				for (int i = 0; i < playerList.size(); i++) {
					PlayerState ps = playerList.get(i);
					labels[i] = ps.getName() + ": " + ps.getPlayState() + " " + ps.getTrackTitle();
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Select player");
				builder.setNegativeButton("Cancel", DLG_CANCEL_CLICK_LISTENER);
				builder.setItems(labels, new DialogInterface.OnClickListener() {
					@Override
					public void onClick (DialogInterface dialog, int item) {
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

	public static void doSearchMlist (final Context context, final PlayerState playerState) {
		doSearchMlist(context, playerState, null);
	}

	public static void doSearchMlist (final Context context, final PlayerState playerState, final AtomicReference<String> defaultQuery) {
		if (context == null) throw new IllegalArgumentException();
		if (playerState == null) throw new IllegalArgumentException();

		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(context);
		dlgBuilder.setTitle("Query " + playerState.getListTitle());

		final EditText editText = new EditText(context);
		editText.setSelectAllOnFocus(true);
		if (defaultQuery != null && defaultQuery.get() != null) editText.setText(defaultQuery.get());
		dlgBuilder.setView(editText);

		DialogInterface.OnClickListener doSearchCall = new DialogInterface.OnClickListener() {
			@Override
			public void onClick (DialogInterface dialog, int whichButton) {
				String query = editText.getText().toString().trim();
				dialog.dismiss();
				if (defaultQuery != null) defaultQuery.set(query); // Save the query for next time.
				if ("".equals(query)) query = "*"; // Default to searching for wild-card.
				searchMlist(context, playerState, query);
			}
		};

		dlgBuilder.setPositiveButton("Search", doSearchCall);
		dlgBuilder.setNegativeButton("Cancel", DLG_CANCEL_CLICK_LISTENER);
		AlertDialog dlg = dlgBuilder.create();
		editText.setOnEditorActionListener(new DlgEnterKeyListener(dlg));
		dlg.show();
	}

	public static void searchMlist (Context context, PlayerState playerState, String query) {
		Intent intent = new Intent(context.getApplicationContext(), MlistActivity.class);
		intent.putExtra(MlistActivity.SERVER_ID, playerState.getPlayerReference().getServerReference().getId());
		intent.putExtra(MlistActivity.MLIST_BASE_URL, playerState.getListUrl());
		intent.putExtra(MlistActivity.QUERY, query);
		intent.putExtra(MlistActivity.PLAYER_ID, playerState.getPlayerReference().getPlayerId());
		context.startActivity(intent);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static void addTag (final Activity activity, final MlistReference mlistReference, final MlistItem item, final Runnable afterPost) {
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(activity);
		dlgBuilder.setTitle("Tag: " + item.getTitle());
		final EditText editText = new EditText(activity);
		dlgBuilder.setView(editText);

		DialogInterface.OnClickListener doAddCall = new DialogInterface.OnClickListener() {
			@Override
			public void onClick (DialogInterface dialog, int whichButton) {
				String tag = editText.getText().toString().trim();
				dialog.dismiss();
				RunMlistItemActionTask task = new RunMlistItemActionTask(activity, mlistReference, item, tag);
				task.setOnComplete(afterPost);
				task.execute();
			}
		};

		dlgBuilder.setPositiveButton("Add", doAddCall);
		dlgBuilder.setNegativeButton("Cancel", DLG_CANCEL_CLICK_LISTENER);
		AlertDialog dlg = dlgBuilder.create();
		editText.setOnEditorActionListener(new DlgEnterKeyListener(dlg));
		dlg.show();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	protected static final DialogInterface.OnClickListener DLG_CANCEL_CLICK_LISTENER = new DialogInterface.OnClickListener() {
		@Override
		public void onClick (DialogInterface dialog, int whichButton) {
			dialog.cancel();
		}
	};

	private static class DlgEnterKeyListener implements OnEditorActionListener {

		private final AlertDialog dlg;

		public DlgEnterKeyListener (AlertDialog dlg) {
			this.dlg = dlg;
		}

		@Override
		public boolean onEditorAction (TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_NULL) {
				this.dlg.getButton(DialogInterface.BUTTON_POSITIVE).dispatchKeyEvent(event);
				return true;
			}
			return false;
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
