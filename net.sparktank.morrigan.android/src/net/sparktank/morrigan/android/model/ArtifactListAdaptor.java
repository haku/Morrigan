package net.sparktank.morrigan.android.model;

import java.util.List;

import android.widget.ListAdapter;

public interface ArtifactListAdaptor extends ListAdapter {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setInputData (List<Artifact> data);
	public List<Artifact> getInputData ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
