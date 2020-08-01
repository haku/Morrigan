package com.vaguehope.morrigan.android.playback;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.vaguehope.morrigan.android.R;

public class MediaListCursorAdapter extends CursorAdapter {

	private final LayoutInflater layoutInflater;

	private final MediaCursorReader cursorReader = new MediaCursorReader();

	public MediaListCursorAdapter (final Context context) {
		super(context, null, false); // Initialise with no cursor.
		this.layoutInflater = LayoutInflater.from(context);
	}

	public void dispose () {
		changeCursor(null);
	}

	private static class RowView {
		TextView text;
		ImageView image;
	}

	@Override
	public View newView (final Context context, final Cursor cursor, final ViewGroup parent) {
		final View view = this.layoutInflater.inflate(R.layout.mlistitemlistrow, null);
		final RowView rowView = new RowView();
		rowView.text = (TextView) view.findViewById(R.id.rowtext);
		rowView.image = (ImageView) view.findViewById(R.id.rowimg);
		view.setTag(rowView);
		return view;
	}

	@Override
	public void bindView (final View view, final Context context, final Cursor cursor) {
		final RowView rowView = (RowView) view.getTag();
		rowView.text.setText(this.cursorReader.readTitle(cursor));
		rowView.image.setImageResource(R.drawable.circledot);
	}

}
