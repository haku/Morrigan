package com.vaguehope.morrigan.android.playback;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.TimeHelper;

public class QueueAdapter extends BaseAdapter {

	private final LayoutInflater layoutInflater;

	private List<QueueItem> listData;

	public QueueAdapter (final Context context) {
		this.layoutInflater = LayoutInflater.from(context);
	}

	public void setInputData(final List<QueueItem> data) {
		this.listData = data;
		notifyDataSetChanged();
	}

	@Override
	public int getCount () {
		if (this.listData == null) return 0;
		return this.listData.size();
	}

	public QueueItem getQueueItem (final int position) {
		if (this.listData == null) return null;
		return this.listData.get(position);
	}

	@Override
	public Object getItem (final int position) {
		return getQueueItem(position);
	}

	@Override
	public long getItemId (final int position) {
		final QueueItem item = getQueueItem(position);
		if (item == null) return -1;
		return item.getQueueId();
	}

	@Override
	public View getView (final int position, final View convertView, final ViewGroup parent) {
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

		final QueueItem item = this.listData.get(position);
		rowView.text.setText(titleForItem(item));
		rowView.image.setImageResource(R.drawable.circledot);

		return view;
	}

	private static CharSequence titleForItem (final QueueItem item) {
		if (item.getDurationMillis() > 0) {
			return String.format("%s (%s)",
					item.getTitle(),
					TimeHelper.formatTimeMiliseconds(item.getDurationMillis()));
		}
		return item.getTitle();
	}

	private static class RowView {
		TextView text;
		ImageView image;
	}

}
