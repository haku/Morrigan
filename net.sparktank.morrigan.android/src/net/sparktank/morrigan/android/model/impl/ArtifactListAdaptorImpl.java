/*
 * Copyright 2010 Alex Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package net.sparktank.morrigan.android.model.impl;

import java.util.Collections;
import java.util.List;

import net.sparktank.morrigan.android.R;
import net.sparktank.morrigan.android.model.Artifact;
import net.sparktank.morrigan.android.model.ArtifactListAdaptor;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ArtifactListAdaptorImpl extends BaseAdapter implements ArtifactListAdaptor {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private List<Artifact> listData;
	
	private LayoutInflater layoutInflater;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public ArtifactListAdaptorImpl (Context context) {
		this.layoutInflater = LayoutInflater.from(context);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ArtifactListAdaptor methods.
	
	@Override
	public void setInputData(List<Artifact> data) {
		this.listData = data;
	}
	
	@Override
	public List<Artifact> getInputData() {
		return Collections.unmodifiableList(this.listData);
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
			rowView.image = (ImageView) view.findViewById(R.id.rowimg);
			
			rowView.image.setImageResource(R.drawable.play);
			
			view.setTag(rowView);
		}
		else {
			rowView = (RowView) view.getTag();
		}
		
		rowView.text.setText(this.listData.get(position).getTitle());
		
		return view;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static protected class RowView {
		TextView text;
		ImageView image;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
