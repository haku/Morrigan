package net.sparktank.morrigan.android.model.impl;

import java.util.List;

import net.sparktank.morrigan.android.R;
import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.ServersListAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ServersListAdapterImpl extends BaseAdapter implements ServersListAdapter {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private List<ServerReference> listData;
	
	private LayoutInflater layoutInflater;
	public Context context;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public ServersListAdapterImpl (Context context) {
		this.layoutInflater = LayoutInflater.from(context);
		this.context = context;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ServersListAdapter methods.
	
	@Override
	public void setInputData(List<ServerReference> data) {
		this.listData = data;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ServersListAdapter methods.
	
	@Override
	public int getCount() {
		return this.listData.size();
	}
	
	@Override
	public Object getItem(int position) {
		return Integer.valueOf(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		RowView rowView;
		
		if (view == null) {
			view = this.layoutInflater.inflate(R.layout.simplelistrow, null);
			
			rowView = new RowView();
			rowView.text = (TextView) view.findViewById(R.id.rowtext);
			
			view.setTag(rowView);
		}
		else {
			rowView = (RowView) view.getTag();
		}
		
		rowView.text.setText(this.listData.get(position).getBaseUrl());
		
		return view;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static protected class RowView {
		TextView text;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
