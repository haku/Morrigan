package net.sparktank.morrigan.android.model;

import java.util.List;

import android.widget.ListAdapter;

public interface ServersListAdapter extends ListAdapter {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setInputData (List<ServerReference> data);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
