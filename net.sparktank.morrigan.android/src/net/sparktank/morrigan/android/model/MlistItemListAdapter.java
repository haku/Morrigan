package net.sparktank.morrigan.android.model;

import android.widget.ListAdapter;

public interface MlistItemListAdapter extends ListAdapter {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void setInputData (MlistItemList data);
	public MlistItemList getInputData ();
	
	public void notifyDataSetChanged ();
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
