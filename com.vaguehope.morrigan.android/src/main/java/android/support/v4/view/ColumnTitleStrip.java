/* Hacked based on android.support.v4.view.PagerTitleStrip.
 * Which is Apache 2 license.
 */

package android.support.v4.view;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.vaguehope.morrigan.android.R;

public class ColumnTitleStrip extends LinearLayout implements ViewPager.Decor /* Decor is package-private :( */{

	// TODO make this a param.
	private static final int BAR_COLOUR = Color.parseColor("#268bd2"); // Solarized Blue.

	public interface ColumnClickListener {
		void onColumnTitleClick (int position);
	}

	private static final int[] ATTRS = new int[] {
			android.R.attr.textSize
	};

	private final int textSizePx;

	private final PageListener mPageListener = new PageListener();
	private ViewPager pager;
	private WeakReference<PagerAdapter> mWatchingAdapter;
	private ColumnClickListener columnClickListener;

	private int lastKnownCurrentPage = 0;
	private float lastKnownPositionOffset = 0;

	private final Paint selectionPaint;
	private final Rect selectionRect;

	private final List<TextView> labels = new ArrayList<TextView>();

	public ColumnTitleStrip (final Context context) {
		this(context, null);
	}

	public ColumnTitleStrip (final Context context, final AttributeSet attrs) {
		super(context, attrs);

		final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
		this.textSizePx = a.getDimensionPixelSize(1, 0);
		a.recycle();

		setOrientation(LinearLayout.HORIZONTAL);

		this.selectionPaint = new Paint(0);
		this.selectionPaint.setColor(BAR_COLOUR);
		this.selectionRect = new Rect(0, 0, 1, 1);
		setWillNotDraw(false);
	}

	public void setViewPager (final ViewPager newPager) {
		if (this.pager != null) {
			updateAdapter(this.pager.getAdapter(), null);
			this.pager.setInternalPageChangeListener(null);
			this.pager.setOnAdapterChangeListener(null);
		}
		this.pager = newPager;
		if (this.pager != null) {
			this.pager.setInternalPageChangeListener(this.mPageListener);
			this.pager.setOnAdapterChangeListener(this.mPageListener);
			updateAdapter(this.mWatchingAdapter != null ? this.mWatchingAdapter.get() : null, this.pager.getAdapter());
		}
	}

	public void setColumnClickListener (final ColumnClickListener listener) {
		this.columnClickListener = listener;
	}

	public ColumnClickListener getColumnClickListener () {
		return this.columnClickListener;
	}

	@Override
	protected void onDetachedFromWindow () {
		super.onDetachedFromWindow();
		setViewPager(null);
	}

	void updateAdapter (final PagerAdapter oldAdapter, final PagerAdapter newAdapter) {
		if (oldAdapter != null) {
			oldAdapter.unregisterDataSetObserver(this.mPageListener);
			this.mWatchingAdapter = null;
		}
		if (newAdapter != null) {
			newAdapter.registerDataSetObserver(this.mPageListener);
			this.mWatchingAdapter = new WeakReference<PagerAdapter>(newAdapter);
			createWidgets();
		}
		if (this.pager != null) {
			this.lastKnownCurrentPage = this.pager.getCurrentItem();
			requestLayout();
		}
	}

	private void createWidgets () {
		final PagerAdapter adapter = this.mWatchingAdapter.get();
		if (adapter == null) return;

		if (this.labels.size() < adapter.getCount()) {
			LayoutInflater inflater = null;
			while (this.labels.size() < adapter.getCount()) {
				if (inflater == null) inflater = LayoutInflater.from(getContext());
				final TextView tv = (TextView) inflater.inflate(R.layout.titlestripbutton, this, false);
				if (this.textSizePx != 0) tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.textSizePx);
				tv.setOnClickListener(new TitleClickListener(this, this.labels.size()));
				this.labels.add(tv);
				addView(tv);
			}
		}
		else if (this.labels.size() > adapter.getCount()) {
			while (this.labels.size() > adapter.getCount()) {
				removeView(this.labels.remove(this.labels.size() - 1));
			}
		}

		for (int i = 0; i < this.labels.size(); i++) {
			final TextView tv = this.labels.get(i);
			tv.setText(adapter.getPageTitle(i));
		}
	}

	private void updateSelectionIndicationPosition () {
		final TextView tv = this.labels.get(this.lastKnownCurrentPage);
		final int offset = (int) (this.lastKnownPositionOffset * tv.getWidth());
		this.selectionRect.left = tv.getLeft() + offset;
		this.selectionRect.top = tv.getTop();
		this.selectionRect.right = tv.getRight() + offset;
		this.selectionRect.bottom = tv.getBottom();
		invalidate();
	}

	@Override
	protected void dispatchDraw (final Canvas canvas) {
		canvas.drawRect(this.selectionRect, this.selectionPaint);
		super.dispatchDraw(canvas); // Now draw children.
	}

	@Override
	protected void onLayout (final boolean changed, final int l, final int t, final int r, final int b) {
		super.onLayout(changed, l, t, r, b);
		updateSelectionIndicationPosition();
	}

	private class PageListener extends DataSetObserver implements ViewPager.OnPageChangeListener, ViewPager.OnAdapterChangeListener {

		public PageListener () {}

		@Override
		public void onPageScrolled (final int position, final float positionOffset, final int positionOffsetPixels) {
			ColumnTitleStrip.this.lastKnownCurrentPage = position;
			ColumnTitleStrip.this.lastKnownPositionOffset = positionOffset;
			updateSelectionIndicationPosition();
		}

		@Override
		public void onPageSelected (final int position) {/* Unused. */}

		@Override
		public void onPageScrollStateChanged (final int state) {/* Unused */}

		@Override
		public void onAdapterChanged (final PagerAdapter oldAdapter, final PagerAdapter newAdapter) {
			updateAdapter(oldAdapter, newAdapter);
		}

		@Override
		public void onChanged () {
			createWidgets();
			updateSelectionIndicationPosition();
		}
	}

	private static class TitleClickListener implements OnClickListener {

		private final ColumnTitleStrip host;
		private final int position;

		public TitleClickListener (final ColumnTitleStrip host, final int position) {
			this.host = host;
			this.position = position;
		}

		@Override
		public void onClick (final View v) {
			this.host.getColumnClickListener().onColumnTitleClick(this.position);
		}

	}

}
