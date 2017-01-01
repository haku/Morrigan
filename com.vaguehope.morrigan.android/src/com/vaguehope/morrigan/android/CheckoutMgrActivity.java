package com.vaguehope.morrigan.android;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.vaguehope.morrigan.android.helper.DialogHelper;
import com.vaguehope.morrigan.android.helper.DialogHelper.Listener;
import com.vaguehope.morrigan.android.helper.StringHelper;
import com.vaguehope.morrigan.android.model.MlistState;
import com.vaguehope.morrigan.android.model.MlistStateList;
import com.vaguehope.morrigan.android.model.MlistStateListChangeListener;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.state.Checkout;
import com.vaguehope.morrigan.android.state.ConfigDb;
import com.vaguehope.morrigan.android.tasks.GetMlistsTask;

public class CheckoutMgrActivity extends Activity {

	private ConfigDb configDb;
	private ArrayAdapter<Checkout> checkoutsAdapter;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.configDb = new ConfigDb(this);
		setContentView(R.layout.checkoutmgr);

		this.checkoutsAdapter = new ArrayAdapter<Checkout>(this, android.R.layout.simple_list_item_1);

		final ListView lstCheckouts = (ListView) findViewById(R.id.lstCheckouts);
		lstCheckouts.setAdapter(this.checkoutsAdapter);
		lstCheckouts.setOnItemClickListener(this.checkoutsListCickListener);
		lstCheckouts.setOnItemLongClickListener(this.checkoutsListLongCickListener);

		final Button btnSyncNow = (Button) findViewById(R.id.btnSyncNow);
		btnSyncNow.setOnClickListener(this.syncNowClickListener);

		reloadCheckouts();
	}

	@Override
	public boolean onCreateOptionsMenu (final Menu menu) {
		getMenuInflater().inflate(R.menu.checkoutmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.newcheckout:
				askAddCheckout();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	protected void reloadCheckouts () {
		this.checkoutsAdapter.clear();
		this.checkoutsAdapter.addAll(this.configDb.getCheckouts());
	}

	private final OnItemClickListener checkoutsListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			askEditCheckout(getCheckoutsAdapter().getItem(position));
		}
	};

	private final OnItemLongClickListener checkoutsListLongCickListener = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick (final AdapterView<?> parent, final View view, final int position, final long id) {
			askDeleteCheckout(getCheckoutsAdapter().getItem(position));
			return true;
		}
	};

	private final OnClickListener syncNowClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			askSyncNow();
		}
	};

	protected ConfigDb getConfigDb () {
		return this.configDb;
	}

	protected ArrayAdapter<Checkout> getCheckoutsAdapter () {
		return this.checkoutsAdapter;
	}

	private void askChooseDb (final ServerReference host, final Listener<MlistState> listener) {
		final Context context = this;
		new GetMlistsTask(context, host, new MlistStateListChangeListener() {
			@Override
			public void onMlistsChange (final MlistStateList result, final Exception e) {
				if (e != null) DialogHelper.alert(CheckoutMgrActivity.this, e);
				if (result != null) DialogHelper.askItem(context, "Select DB", new ArrayList<MlistState>(result.getMlistStateList()), listener);
			}
		}).execute();
	}

	private void askAddCheckout () {
		DialogHelper.askItem(this, "Select Server", this.configDb.getHosts(), new Listener<ServerReference>() {
			@Override
			public void onAnswer (final ServerReference answer) {
				askAddCheckout(answer);
			}
		});
	}

	protected void askAddCheckout (final ServerReference host) {
		askChooseDb(host, new Listener<MlistState>() {
			@Override
			public void onAnswer (final MlistState answer) {
				askAddCheckout(host, answer);
			}
		});
	}

	protected void askAddCheckout (final ServerReference host, final MlistState db) {
		final CheckoutDlg dlg = new CheckoutDlg(this, host);
		dlg.getBldr().setPositiveButton("Add", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				if (!dlg.isFilledIn()) return;
				dialog.dismiss();
				getConfigDb().addCheckout(new Checkout(null, host.getId(), db.getRelativePath(), dlg.getQuery(), dlg.getLocalDir(), ""));
				reloadCheckouts();
			}
		});
		dlg.show();
	}

	protected void askEditCheckout (final Checkout checkout) {
		final ServerReference host = this.configDb.getServer(checkout.getHostId());
		if (host == null) {
			DialogHelper.askItem(this, "Replace server", this.configDb.getHosts(), new Listener<ServerReference>() {
				@Override
				public void onAnswer (final ServerReference answer) {
					askEditCheckout(checkout.withHostId(answer.getId()));
				}
			});
			return;
		}

		if (StringHelper.isEmpty(checkout.getDbRelativePath())) {
			askChooseDb(host, new Listener<MlistState>() {
				@Override
				public void onAnswer (final MlistState answer) {
					askEditCheckout(checkout.withDbRelativePath(answer.getRelativePath()));
				}
			});
			return;
		}

		final CheckoutDlg dlg = new CheckoutDlg(this, host, checkout);
		dlg.getBldr().setPositiveButton("Update", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				if (!dlg.isFilledIn()) return;
				dialog.dismiss();
				getConfigDb().updateCheckout(checkout
						.withQuery(dlg.getQuery())
						.withLocalDir(dlg.getLocalDir()));
				reloadCheckouts();
			}
		});
		dlg.show();
	}

	protected void askDeleteCheckout (final Checkout checkout) {
		DialogHelper.askYesNo(this, "Delete checkout?", "Delete", "Keep", new Runnable() {
			@Override
			public void run () {
				getConfigDb().removeCheckout(checkout);
				reloadCheckouts();
			}
		});
	}

	private static class CheckoutDlg {

		private final AlertDialog.Builder bldr;
		private final EditText txtQuery;
		private final EditText txtLocalDir;

		public CheckoutDlg (final Context context, final ServerReference host) {
			this(context, host, null);
		}

		public CheckoutDlg (final Context context, final ServerReference host, final Checkout checkout) {
			this.bldr = new AlertDialog.Builder(context);
			this.bldr.setTitle(String.format("%s Checkout", host.getName()));

			this.txtQuery = new EditText(context);
			this.txtQuery.setHint("query");

			this.txtLocalDir = new EditText(context);
			this.txtLocalDir.setHint("local directory");

			if (checkout != null) {
				this.txtQuery.setText(checkout.getQuery());
				this.txtLocalDir.setText(checkout.getLocalDir());
			}

			final LinearLayout layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.addView(this.txtQuery);
			layout.addView(this.txtLocalDir);
			this.bldr.setView(layout);

			this.bldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick (final DialogInterface dialog, final int whichButton) {
					dialog.cancel();
				}
			});
		}

		public AlertDialog.Builder getBldr () {
			return this.bldr;
		}

		public boolean isFilledIn () {
			return !StringHelper.isEmpty(getQuery()) && !StringHelper.isEmpty(getLocalDir());
		}

		public String getQuery () {
			return this.txtQuery.getText().toString().trim();
		}

		public String getLocalDir () {
			return this.txtLocalDir.getText().toString().trim();
		}

		public void show () {
			this.bldr.show();
		}

	}

	protected void askSyncNow () {
		DialogHelper.askYesNo(this, "Sync now?", "Sync", "Cancel", new Runnable() {
			@Override
			public void run () {
				startSync();
			}
		});
	}

	protected void startSync () {
		startService(new Intent(this, SyncCheckoutsService.class));
	}

}
