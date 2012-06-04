package com.vaguehope.morrigan.android;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.vaguehope.morrigan.android.model.ArtifactListAdaptor;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.model.ServerReferenceList;
import com.vaguehope.morrigan.android.modelimpl.ArtifactListAdaptorImpl;
import com.vaguehope.morrigan.android.state.ConfigDb;

public class ServersList {

	private final ArtifactListAdaptor<ServerReferenceList> serversListAdapter;
	private final ServerListEventsListener eventsListener;

	public ServersList (Context context, ListView listView, ConfigDb configDb, ServerListEventsListener eventsListener) {
		this.eventsListener = eventsListener;
		this.serversListAdapter = new ArtifactListAdaptorImpl<ServerReferenceList>(context, R.layout.simplelistrow);
		this.getServersListAdapter().setInputData(configDb);
		listView.setAdapter(this.getServersListAdapter());
		listView.setOnItemClickListener(this.serversListCickListener);
//		lstServers.setOnCreateContextMenuListener(this.serversContextMenuListener);
	}

	public ServerListEventsListener getEventsListener () {
		return this.eventsListener;
	}

	public ArtifactListAdaptor<ServerReferenceList> getServersListAdapter () {
		return this.serversListAdapter;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private OnItemClickListener serversListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
			ServerReference item = getServersListAdapter().getInputData().getServerReferenceList().get(position);
			getEventsListener().showServer(item);
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public interface ServerListEventsListener {
		void showServer (ServerReference ref);
	}

}
