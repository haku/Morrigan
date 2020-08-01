package com.vaguehope.morrigan.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.vaguehope.morrigan.android.model.ArtifactListAdaptor;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.model.ServerReferenceList;
import com.vaguehope.morrigan.android.modelimpl.ArtifactListAdaptorImpl;
import com.vaguehope.morrigan.android.modelimpl.ServerReferenceImpl;
import com.vaguehope.morrigan.android.state.ConfigDb;

public class ServersList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Private constants.

	private static final int MENU_EDIT = 1;
	private static final int MENU_DELETE = 2;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	State.

	private final Context context;
	private final ConfigDb configDb;
	private final ArtifactListAdaptor<ServerReferenceList> serversListAdapter;
	private final ServerListEventsListener eventsListener;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public ServersList (Context context, ListView listView, ConfigDb configDb, ServerListEventsListener eventsListener) {
		this.context = context;
		this.configDb = configDb;
		this.eventsListener = eventsListener;
		this.serversListAdapter = new ArtifactListAdaptorImpl<ServerReferenceList>(context, R.layout.simplelistrow);
		this.getServersListAdapter().setInputData(configDb);
		listView.setAdapter(this.getServersListAdapter());
		listView.setOnItemClickListener(this.serversListCickListener);
		listView.setOnCreateContextMenuListener(this.serversContextMenuListener);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	API.

	public Context getContext () {
		return this.context;
	}

	public ConfigDb getConfigDb () {
		return this.configDb;
	}

	public ServerListEventsListener getEventsListener () {
		return this.eventsListener;
	}

	public ArtifactListAdaptor<ServerReferenceList> getServersListAdapter () {
		return this.serversListAdapter;
	}

	public void promptAddServer () {
		addServer();
	}

	public boolean handleOnContextItemSelected(MenuItem item) {
		return onContextItemSelected(item);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private OnItemClickListener serversListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
			ServerReference item = getServersListAdapter().getInputData().getServerReferenceList().get(position);
			getEventsListener().showServer(item);
		}
	};

	private OnCreateContextMenuListener serversContextMenuListener = new OnCreateContextMenuListener () {
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			ServerReference item = getServersListAdapter().getInputData().getServerReferenceList().get(info.position);
			menu.setHeaderTitle(item.getName());
			menu.add(Menu.NONE, MENU_EDIT, Menu.NONE, "Edit");
			menu.add(Menu.NONE, MENU_DELETE, Menu.NONE, "Remove");
		}
	};

	private boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		ServerReference ref;

		switch (item.getItemId()) {
			case MENU_EDIT:
				info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
				ref = getServersListAdapter().getInputData().getServerReferenceList().get(info.position);
				editServer(ref);
				return true;

			case MENU_DELETE:
				info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
				ref = getServersListAdapter().getInputData().getServerReferenceList().get(info.position);
				deleteServer(ref);
				return true;

			default:
				return false;
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.

	private void addServer () {
		final ServerDlg dlg = new ServerDlg(this.context);
		dlg.getBldr().setPositiveButton("Add", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				if (!dlg.isSet()) {
					dialog.cancel();
					return;
				}
				dialog.dismiss();

				getConfigDb().addServer(new ServerReferenceImpl(dlg.getName(), dlg.getUrl(), dlg.getPass()));
				getServersListAdapter().notifyDataSetChanged();
			}
		});
		dlg.show();
	}

	private void editServer (final ServerReference ref) {
		final ServerDlg dlg = new ServerDlg(this.context, ref);
		dlg.getBldr().setPositiveButton("Update", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				if (!dlg.isSet()) {
					dialog.cancel();
					return;
				}
				dialog.dismiss();

				getConfigDb().updateServer(new ServerReferenceImpl(ref.getId(), dlg.getName(), dlg.getUrl(), dlg.getPass()));
				getServersListAdapter().notifyDataSetChanged();
			}
		});
		dlg.show();
	}
	private void deleteServer (final ServerReference sr) {
		final AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this.context);
		dlgBuilder.setMessage("Delete: " + sr.getTitle());

		dlgBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
			@Override
			public void onClick (DialogInterface dialog, int which) {
				getConfigDb().removeServer(sr);
				getServersListAdapter().notifyDataSetChanged();
				Toast.makeText(getContext(), "Removed: " + sr.getBaseUrl(), Toast.LENGTH_SHORT).show();
			}
		});

		dlgBuilder.setNegativeButton("Keep", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.cancel();
			}
		});

		dlgBuilder.show();
	}

	private static class ServerDlg {

		private final AlertDialog.Builder bldr;
		private final EditText txtName;
		private final EditText txtUrl;
		private final EditText txtPass;

		public ServerDlg (Context context) {
			this(context, null);
		}

		public ServerDlg (Context context, ServerReference ref) {
			this.bldr = new AlertDialog.Builder(context);
			this.bldr.setTitle("Server");

			this.txtName = new EditText(context);
			this.txtName.setHint("name");
			if (ref != null) this.txtName.setText(ref.getName());

			this.txtUrl = new EditText(context);
			this.txtUrl.setHint("url");
			this.txtUrl.setText(ref != null ? ref.getBaseUrl() : "http://host:8080");

			this.txtPass = new EditText(context);
			this.txtPass.setHint("pass");
			if (ref != null) this.txtPass.setText(ref.getPass());

			LinearLayout layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);
			layout.addView(this.txtName);
			layout.addView(this.txtUrl);
			layout.addView(this.txtPass);
			this.bldr.setView(layout);

			this.bldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int whichButton) {
					dialog.cancel();
				}
			});
		}

		public AlertDialog.Builder getBldr () {
			return this.bldr;
		}

		public boolean isSet () {
			String name = getName();
			String url = getUrl();
			String pass = getPass();
			return (name != null && name.length() > 0
					&& url != null && url.length() > 0
					&& pass != null && pass.length() > 0
					);
		}

		public String getName () {
			return this.txtName.getText().toString().trim();
		}

		public String getUrl () {
			return this.txtUrl.getText().toString().trim();
		}

		public String getPass () {
			return this.txtPass.getText().toString().trim();
		}

		public void show () {
			this.bldr.show();
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public interface ServerListEventsListener {
		void showServer (ServerReference ref);
	}

}
