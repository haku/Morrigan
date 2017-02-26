package com.vaguehope.morrigan.android.state;

import java.util.Map;

import com.vaguehope.morrigan.android.model.ServerReference;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CheckoutAdapter extends ArrayAdapter<Checkout> {

	private static final int RESOURCE = android.R.layout.simple_list_item_1;

	private final Map<String, ServerReference> hosts;
	private final LayoutInflater layoutInflater;

	public CheckoutAdapter (final Context context, final Map<String, ServerReference> hosts) {
		super(context, RESOURCE);
		this.hosts = hosts;
		this.layoutInflater = LayoutInflater.from(context);
	}

	@Override
	public View getView (final int position, final View convertView, final ViewGroup parent) {
		final TextView view;
		if (convertView == null) {
			view = (TextView) this.layoutInflater.inflate(RESOURCE, parent, false);
		}
		else {
			view = (TextView) convertView;
		}
		view.setText(getItem(position).toString(this.hosts));
		return view;
	}

}
