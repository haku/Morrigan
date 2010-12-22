package net.sparktank.morrigan.android.model.impl;

import java.util.List;

import net.sparktank.morrigan.android.R;
import net.sparktank.morrigan.android.model.MlistItem;
import net.sparktank.morrigan.android.model.MlistItemList;
import net.sparktank.morrigan.android.model.MlistItemListAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MlistItemListAdaptorImpl extends BaseAdapter implements MlistItemListAdapter {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private MlistItemList listData;
	
	private LayoutInflater layoutInflater;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public MlistItemListAdaptorImpl (Context context) {
		this.layoutInflater = LayoutInflater.from(context);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MlistItemListAdapter methods.
	
	@Override
	public void setInputData(MlistItemList data) {
		this.listData = data;
		notifyDataSetChanged();
	}
	
	@Override
	public MlistItemList getInputData() {
		return this.listData;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ServersListAdapter methods.
	
	@Override
	public int getCount() {
		if (this.listData == null) return 0;
		List<? extends MlistItem> mlistItemList = this.listData.getMlistItemList();
		if (mlistItemList == null) return 0;
		return mlistItemList.size();
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
			view = this.layoutInflater.inflate(R.layout.mlistitemlistrow, null);
			
			rowView = new RowView();
			rowView.text = (TextView) view.findViewById(R.id.rowtext);
			rowView.image = (ImageView) view.findViewById(R.id.rowimg);
			
			view.setTag(rowView);
		}
		else {
			rowView = (RowView) view.getTag();
		}
		
		MlistItem item = this.listData.getMlistItemList().get(position);
		rowView.text.setText(item.getTitle());
		rowView.image.setImageResource(item.getImageResource());
		
		return view;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static protected class RowView {
		TextView text;
		ImageView image;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
