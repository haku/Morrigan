package com.vaguehope.morrigan.android.checkout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.DialogHelper;
import com.vaguehope.morrigan.android.helper.DialogHelper.Listener;
import com.vaguehope.morrigan.android.helper.StringHelper;
import com.vaguehope.morrigan.android.model.MlistState;
import com.vaguehope.morrigan.android.model.MlistStateList;
import com.vaguehope.morrigan.android.model.MlistStateListChangeListener;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.state.Checkout;
import com.vaguehope.morrigan.android.state.CheckoutAdapter;
import com.vaguehope.morrigan.android.state.ConfigDb;
import com.vaguehope.morrigan.android.tasks.GetMlistsTask;

public class CheckoutMgrActivity extends Activity {

	private ConfigDb configDb;
	private final Map<String, ServerReference> allHosts = new HashMap<String, ServerReference>();
	private ArrayAdapter<Checkout> checkoutsAdapter;

	@Override
	public void onCreate (final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.configDb = new ConfigDb(this);
		setContentView(R.layout.checkoutmgr);

		this.checkoutsAdapter = new CheckoutAdapter(this, this.allHosts);

		final ListView lstCheckouts = (ListView) findViewById(R.id.lstCheckouts);
		lstCheckouts.setAdapter(this.checkoutsAdapter);
		lstCheckouts.setOnItemClickListener(this.checkoutsListCickListener);
		lstCheckouts.setOnItemLongClickListener(this.checkoutsListLongCickListener);

		((Button) findViewById(R.id.btnSyncServer)).setOnClickListener(this.syncServerClickListener);
		((Button) findViewById(R.id.btnSyncAll)).setOnClickListener(this.syncAllClickListener);

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
			case R.id.cleanold:
				askCleanOld();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	protected void reloadCheckouts () {
		for (final ServerReference h : this.configDb.getHosts()) {
			this.allHosts.put(h.getId(), h);
		}

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

	private final OnClickListener syncServerClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			askSyncServer();
		}
	};

	private final OnClickListener syncAllClickListener = new OnClickListener() {
		@Override
		public void onClick (final View v) {
			askSyncAll();
		}
	};

	protected ConfigDb getConfigDb () {
		return this.configDb;
	}

	protected ArrayAdapter<Checkout> getCheckoutsAdapter () {
		return this.checkoutsAdapter;
	}

	private static void askChooseAHost (final Context context, final ConfigDb configDb, final Listener<ServerReference> listener) {
		DialogHelper.askItem(context, "Select Server", configDb.getHosts(), new Listener<ServerReference>() {
			@Override
			public void onAnswer (final ServerReference answer) {
				listener.onAnswer(answer);
			}
		});
	}

	private static void askChooseADb (final Context context, final ServerReference host, final Listener<MlistState> listener) {
		new GetMlistsTask(context, host, new MlistStateListChangeListener() {
			@Override
			public void onMlistsChange (final MlistStateList result, final Exception e) {
				if (e != null) DialogHelper.alert(context, e);
				if (result != null) DialogHelper.askItem(context, "Select DB", new ArrayList<MlistState>(result.getMlistStateList()), listener);
			}
		}).execute();
	}

	private void askAddCheckout () {
		askChooseAHost(this, this.configDb, new Listener<ServerReference>() {
			@Override
			public void onAnswer (final ServerReference answer) {
				askAddCheckoutForHost(answer);
			}
		});
	}

	protected void askAddCheckoutForHost (final ServerReference host) {
		askChooseADb(this, host, new Listener<MlistState>() {
			@Override
			public void onAnswer (final MlistState db) {
				askAddCheckoutForHostAndDb(host, db);
			}
		});
	}

	protected void askAddCheckoutForHostAndDb (final ServerReference host, final MlistState db) {
		final CheckoutDlg dlg = new CheckoutDlg(this, this.configDb, host, db.getRelativePath());
		dlg.getBldr().setPositiveButton("Add", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				if (!dlg.isFilledIn()) return;
				dialog.dismiss();
				getConfigDb().addCheckout(new Checkout(null,
						dlg.getHostId(), dlg.getDbRelativePath(),
						dlg.getQuery(), dlg.getLocalDir(),
						""));
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
			askChooseADb(this, host, new Listener<MlistState>() {
				@Override
				public void onAnswer (final MlistState answer) {
					askEditCheckout(checkout.withDbRelativePath(answer.getRelativePath()));
				}
			});
			return;
		}

		final CheckoutDlg dlg = new CheckoutDlg(this, this.configDb, host, checkout);
		dlg.getBldr().setPositiveButton("Update", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (final DialogInterface dialog, final int whichButton) {
				if (!dlg.isFilledIn()) return;
				dialog.dismiss();
				getConfigDb().updateCheckout(checkout
						.withHostId(dlg.getHostId())
						.withDbRelativePath(dlg.getDbRelativePath())
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

		private final Context context;
		private final ConfigDb configDb;
		private final AlertDialog.Builder bldr;
		private AlertDialog dialog;
		private final Button btnServerAndDb;
		private String selectedHostId;
		private String selectedDbRelativePath;
		private final EditText txtQuery;
		private final EditText txtLocalDir;

		public CheckoutDlg (final Context context, final ConfigDb configDb, final ServerReference host, final String dbRelativePath) {
			this(context, configDb, host, dbRelativePath, null, null);
		}

		public CheckoutDlg (final Context context, final ConfigDb configDb, final ServerReference host, final Checkout checkout) {
			this(context, configDb, host, checkout.getDbRelativePath(), checkout.getQuery(), checkout.getLocalDir());
		}

		public CheckoutDlg (final Context context, final ConfigDb configDb, final ServerReference host, final String dbRelativePath, final String query, final String localDir) {
			this.context = context;
			this.configDb = configDb;

			this.bldr = new AlertDialog.Builder(context);
			this.bldr.setTitle(String.format("%s Checkout", host.getName()));

			this.btnServerAndDb = new Button(context);
			this.btnServerAndDb.setOnClickListener(this.btnServerAndDbOnClickListener);

			this.txtQuery = new EditText(context);
			this.txtQuery.setHint("query");
			this.txtQuery.addTextChangedListener(this.textWatcher);

			this.txtLocalDir = new EditText(context);
			this.txtLocalDir.setHint("local directory");
			this.txtLocalDir.addTextChangedListener(this.textWatcher);

			setSelctedHostAndDb(host, dbRelativePath);
			if (query != null) this.txtQuery.setText(query);
			if (localDir != null) this.txtLocalDir.setText(localDir);

			final LinearLayout layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.addView(this.btnServerAndDb);
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

		private void setSelctedHostAndDb (final ServerReference host, final String dbRelativePath) {
			this.selectedHostId = host.getId();
			this.selectedDbRelativePath = dbRelativePath;
			this.btnServerAndDb.setText(String.format("%s\n%s",
					host.getName(),
					dbRelativePath));
		}

		private final TextWatcher textWatcher = new TextWatcher() {
			@Override
			public void afterTextChanged (final Editable s) {
				if (CheckoutDlg.this.dialog == null) return;
				CheckoutDlg.this.dialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(isFilledIn());
			}
			@Override
			public void onTextChanged (final CharSequence s, final int start, final int before, final int count) {}
			@Override
			public void beforeTextChanged (final CharSequence s, final int start, final int count, final int after) {}
		};

		private final OnClickListener btnServerAndDbOnClickListener = new OnClickListener() {
			@Override
			public void onClick (final View v) {
				askChooseAHost(CheckoutDlg.this.context, CheckoutDlg.this.configDb, new Listener<ServerReference>() {
					@Override
					public void onAnswer (final ServerReference host) {
						askChooseADb(CheckoutDlg.this.context, host, new Listener<MlistState>() {
							@Override
							public void onAnswer (final MlistState db) {
								setSelctedHostAndDb(host, db.getRelativePath());
							}
						});
					}
				});
			}
		};

		public AlertDialog.Builder getBldr () {
			return this.bldr;
		}

		public boolean isFilledIn () {
			return StringHelper.notEmpty(this.selectedHostId)
					&& StringHelper.notEmpty(this.selectedDbRelativePath)
					&& StringHelper.notEmpty(getQuery())
					&& StringHelper.notEmpty(getLocalDir());
		}

		public String getHostId () {
			return this.selectedHostId;
		}

		public String getDbRelativePath () {
			return this.selectedDbRelativePath;
		}

		public String getQuery () {
			return this.txtQuery.getText().toString().trim();
		}

		public String getLocalDir () {
			return this.txtLocalDir.getText().toString().trim();
		}

		public void show () {
			this.dialog = this.bldr.show();
			this.textWatcher.afterTextChanged(null);
		}

	}

	private void askCleanOld () {
		final Set<String> excessFiles;
		try {
			excessFiles = CheckoutIndex.findExcessFiles(this, this.configDb.getCheckouts());
		}
		catch (final IOException e) {
			DialogHelper.alert(this, e);
			return;
		}

		final List<TitleableFile> items = new ArrayList<TitleableFile>(excessFiles.size());
		for (final String path : excessFiles) {
			items.add(new TitleableFile(path));
		}

		DialogHelper.askItems(this, "Select to Delete (" + items.size() + ")", items, new Listener<Set<TitleableFile>>() {
			@Override
			public void onAnswer (final Set<TitleableFile> toDelete) {
				deleteFiles(toDelete);
			}
		});
	}

	protected void deleteFiles (final Set<TitleableFile> items) {
		for (final TitleableFile item : items) {
			final File file = item.getFile();
			if (!file.delete()) {
				DialogHelper.alert(this, "Failed to delete: " + file.getAbsolutePath());
				break;
			}
		}
		DialogHelper.alert(this, "Deleted " + items.size() + " items.");
	}

	protected void askSyncServer () {
		Map<String, ServerReference> allHosts = new HashMap<String, ServerReference>();
		for (ServerReference h : this.configDb.getHosts()) {
			allHosts.put(h.getId(), h);
		}

		final Map<String, ServerReference> usedHosts = new TreeMap<String, ServerReference>();
		for (Checkout co : this.configDb.getCheckouts()) {
			usedHosts.put(co.getHostId(), allHosts.get(co.getHostId()));
		}

		DialogHelper.askItem(this, "Select Server to sync", new ArrayList<ServerReference>(usedHosts.values()), new Listener<ServerReference>() {
			@Override
			public void onAnswer (final ServerReference answer) {
				startSync(answer.getId());
			}
		});
	}

	protected void askSyncAll () {
		DialogHelper.askYesNo(this, "Sync all hosts now?", "Sync", "Cancel", new Runnable() {
			@Override
			public void run () {
				startSync(null);
			}
		});
	}

	protected void startSync (final String hostId) {
		final Intent i = new Intent(this, SyncCheckoutsService.class);
		if (hostId != null) i.putExtra(SyncCheckoutsService.EXTRA_HOST_SYNC, hostId);
		startService(i);
	}

}
