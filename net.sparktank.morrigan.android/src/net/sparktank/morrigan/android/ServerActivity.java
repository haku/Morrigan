package net.sparktank.morrigan.android;

import java.util.ArrayList;
import java.util.List;

import net.sparktank.morrigan.android.model.Artifact;
import net.sparktank.morrigan.android.model.ArtifactListAdaptor;
import net.sparktank.morrigan.android.model.PlayerArtifact;
import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.impl.ArtifactListAdaptorImpl;
import net.sparktank.morrigan.android.model.impl.PlayerArtifactImpl;
import net.sparktank.morrigan.android.model.impl.ServerReferenceImpl;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class ServerActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String BASE_URL = "baseUrl";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	ServerReference serverReference = null;
	ArtifactListAdaptor artifactListAdaptor;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		String baseUrl = extras.getString(BASE_URL);
		if (baseUrl != null) {
			this.serverReference = new ServerReferenceImpl(baseUrl); // TODO use data passed into activity to get ServerReference from DB.
		}
		else {
			finish();
		}
		this.setTitle(baseUrl);
		
		setContentView(R.layout.server);
		wireGui();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	GUI methods.
	
	private void wireGui () {
		this.artifactListAdaptor = new ArtifactListAdaptorImpl(this);
		
		// FIXME temp test data.
		List<Artifact> data = new ArrayList<Artifact>();
		data.add(new PlayerArtifactImpl(0));
		this.artifactListAdaptor.setInputData(data);
		
		ListView lstServers = (ListView) findViewById(R.id.lstServer);
		lstServers.setAdapter(this.artifactListAdaptor);
		lstServers.setOnItemClickListener(this.artifactsListCickListener);
		
		ImageButton cmd;
		
		cmd = (ImageButton) findViewById(R.id.btnRefresh);
		cmd.setOnClickListener(new BtnRefresh_OnClick());
		
		cmd = (ImageButton) findViewById(R.id.btnSearch);
		cmd.setOnClickListener(new BtnSearch_OnClick());
	}
	
	private OnItemClickListener artifactsListCickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Artifact item = ServerActivity.this.artifactListAdaptor.getInputData().get(position);
			showArtifactActivity(item);
		}
	};
	
	class BtnRefresh_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			refresh();
		}
	}
	
	class BtnSearch_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			search();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Commands - to be called on the UI thread.
	
	protected void showArtifactActivity (Artifact item) {
		if (item instanceof PlayerArtifact) {
			PlayerArtifact pa = (PlayerArtifact) item;
			showArtifactActivity(pa);
		}
		else {
			throw new IllegalArgumentException();
		}
	}
	
	protected void showArtifactActivity (PlayerArtifact item) {
		Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
		intent.putExtra(PlayerActivity.BASE_URL, this.serverReference.getBaseUrl());
		intent.putExtra(PlayerActivity.PLAYER_ID, item.getId());
		startActivity(intent);
	}
	
	protected void refresh () {
		Toast.makeText(this, "TODO: refresh", Toast.LENGTH_SHORT).show();
	}
	
	protected void search () {
		Toast.makeText(this, "TODO: search", Toast.LENGTH_SHORT).show();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
