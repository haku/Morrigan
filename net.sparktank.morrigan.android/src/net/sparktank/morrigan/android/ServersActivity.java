package net.sparktank.morrigan.android;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.ServersListAdapter;
import net.sparktank.morrigan.android.model.impl.ServerReferenceImpl;
import net.sparktank.morrigan.android.model.impl.ServersListAdapterImpl;
import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

public class ServersActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	ServersListAdapter serversListAdapter;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.servers);
		
		wireGui();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI methods.
	
	private void wireGui () {
		this.serversListAdapter = new ServersListAdapterImpl(this);
		
		// FIXME temp test data.
		List<ServerReference> data = new ArrayList<ServerReference>();
		data.add(new ServerReferenceImpl(TempConstants.serverUrl));
		this.serversListAdapter.setInputData(data);
		
		ListView lstServers = (ListView) findViewById(R.id.lstServers);
		lstServers.setAdapter(this.serversListAdapter);
		// lstServers.setOnItemClickListener(inventoryAdapter);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
