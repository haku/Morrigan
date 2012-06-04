/* Original author's notice is below.  This class has been heavily modified.
 *
 * Copyright (C) 2012 0xlab - http://0xlab.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authored by Julian Chu <walkingice AT 0xlab.org>
 */

package com.vaguehope.morrigan.android.layouts;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import com.vaguehope.morrigan.android.R;

public class SidebarLayout extends ViewGroup {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final static int SLIDE_DURATION = 300; // 0.3 seconds?

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	properties.


	private int sidebarViewRes;
	private int hostViewRes;
	private View sidebarView;
	private View hostView;
	private SidebarListener listener;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	state.

	private boolean sidebarOpen;
	private boolean pressed = false;
	private int sidebarWidth = 150; // assign default value. It will be overwrite in onMeasure by Layout XML resource.
	private Animation animation;
	private OpenListener openListener;
	private CloseListener closeListener;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public SidebarLayout (Context context) {
		this(context, null);
	}

	public SidebarLayout (Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SidebarLayout);
		this.hostViewRes = a.getResourceId(R.styleable.SidebarLayout_hostView, -1);
		this.sidebarViewRes = a.getResourceId(R.styleable.SidebarLayout_sidebarView, -1);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public View getHostView () {
		if (this.hostView == null && this.hostViewRes > 0) {
			this.hostView = findViewById(this.hostViewRes);
		}
		if (this.hostView == null) throw new IllegalStateException("Host view is not set.");
		return this.hostView;
	}

	public void setHostView (View hostView) {
		if (hostView == null) throw new IllegalArgumentException("Host view can not be null.");
		this.hostView = hostView;
	}

	public View getSidebarView () {
		if (this.sidebarView == null && this.sidebarViewRes > 0) {
			this.sidebarView = findViewById(this.sidebarViewRes);
		}
		if (this.sidebarView == null) throw new IllegalStateException("Side bar view is not set.");
		return this.sidebarView;
	}

	public void setSidebarView (View sidebarView) {
		if (sidebarView == null) throw new IllegalArgumentException("Side bar view can not be null.");
		this.sidebarView = sidebarView;
	}

	public SidebarListener getListener () {
		return this.listener;
	}

	public void setListener (SidebarListener l) {
		this.listener = l;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public boolean isOpen () {
		return this.sidebarOpen;
	}

	protected void setOpen (boolean open) {
		this.sidebarOpen = open;
	}

	public void toggleSidebar () {
		if (this.getHostView().getAnimation() != null) return;

		if (this.sidebarOpen) {
			this.animation = new TranslateAnimation(0, -this.sidebarWidth, 0, 0);
			this.animation.setAnimationListener(this.closeListener);
		}
		else {
			this.animation = new TranslateAnimation(0, this.sidebarWidth, 0, 0);
			this.animation.setAnimationListener(this.openListener);
		}
		this.animation.setDuration(SLIDE_DURATION);
		this.animation.setFillAfter(true);
		this.animation.setFillEnabled(true);
		this.getHostView().startAnimation(this.animation);
	}

	public boolean openSidebar () {
		if (!this.sidebarOpen) {
			toggleSidebar();
			return true;
		}
		return false;
	}

	public boolean closeSidebar () {
		if (this.sidebarOpen) {
			toggleSidebar();
			return true;
		}
		return false;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void onFinishInflate () {
		super.onFinishInflate();
		this.openListener = new OpenListener(this);
		this.closeListener = new CloseListener(this);
	}

	@Override
	public void onLayout (boolean changed, int l, int t, int r, int b) {
		// The title bar assign top padding, drop it.
		this.getSidebarView().layout(l, 0, l + this.sidebarWidth, 0 + this.getSidebarView().getMeasuredHeight());
		if (this.sidebarOpen) {
			this.getHostView().layout(l + this.sidebarWidth, 0, r + this.sidebarWidth, b);
		}
		else {
			this.getHostView().layout(l, 0, r, b);
		}
	}

	@Override
	public void onMeasure (int w, int h) {
		super.onMeasure(w, h);
		super.measureChildren(w, h);
		this.sidebarWidth = this.getSidebarView().getMeasuredWidth();
	}

	@Override
	protected void measureChild (View child, int parentWSpec, int parentHSpec) {
		// The max width of side bar is 90% of Parent.
		if (child == this.getSidebarView()) {
			int mode = MeasureSpec.getMode(parentWSpec);
			int width = (int) (getMeasuredWidth() * 0.9);
			super.measureChild(child, MeasureSpec.makeMeasureSpec(width, mode), parentHSpec);
		}
		else {
			super.measureChild(child, parentWSpec, parentHSpec);
		}
	}

	@Override
	public boolean onInterceptTouchEvent (MotionEvent ev) {
		if (!isOpen()) return false;

		int action = ev.getAction();
		if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_DOWN) {
			return false;
		}

		// if user press and release both on Content while side bar is opening,
		// call listener. otherwise, pass the event to child.
		int x = (int) ev.getX();
		int y = (int) ev.getY();
		if (this.getHostView().getLeft() < x && this.getHostView().getRight() > x && this.getHostView().getTop() < y && this.getHostView().getBottom() > y) {
			if (action == MotionEvent.ACTION_DOWN) {
				this.pressed = true;
			}

			if (this.pressed && action == MotionEvent.ACTION_UP && this.listener != null) {
				this.pressed = false;
				return this.listener.onContentTouchedWhenOpening(SidebarLayout.this);
			}
		}
		else {
			this.pressed = false;
		}

		return false;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private class OpenListener implements Animation.AnimationListener {

		private final SidebarLayout sidebarLayout;

		public OpenListener (SidebarLayout sidebarLayout) {
			this.sidebarLayout = sidebarLayout;
		}

		@Override
		public void onAnimationStart (Animation a) {
			this.sidebarLayout.getSidebarView().setVisibility(View.VISIBLE);
		}

		@Override
		public void onAnimationEnd (Animation a) {
			this.sidebarLayout.getHostView().clearAnimation();
			setOpen(!isOpen());
			requestLayout();
			SidebarListener l = getListener();
			if (l != null) l.onSidebarOpened(SidebarLayout.this);
		}

		@Override
		public void onAnimationRepeat (Animation a) {/* Unused. */}
	}

	private class CloseListener implements Animation.AnimationListener {

		private final SidebarLayout sidebarLayout;

		public CloseListener (SidebarLayout sidebarLayout) {
			this.sidebarLayout = sidebarLayout;
		}

		@Override
		public void onAnimationEnd (Animation a) {
			this.sidebarLayout.getHostView().clearAnimation();
			this.sidebarLayout.getSidebarView().setVisibility(View.INVISIBLE);
			setOpen(!isOpen());
			requestLayout();
			SidebarListener l = getListener();
			if (l != null) l.onSidebarClosed(SidebarLayout.this);
		}

		@Override
		public void onAnimationRepeat (Animation a) {/* Unused. */}

		@Override
		public void onAnimationStart (Animation a) {/* Unused. */}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public interface SidebarListener {

		public void onSidebarOpened (SidebarLayout sidebar);

		public void onSidebarClosed (SidebarLayout sidebar);

		public boolean onContentTouchedWhenOpening (SidebarLayout sidebar);

	}

	public static class ToggleSidebarListener implements OnClickListener {

		private final SidebarLayout sidebar;

		public ToggleSidebarListener (SidebarLayout sidebar) {
			this.sidebar = sidebar;
		}

		@Override
		public void onClick (View v) {
			this.sidebar.toggleSidebar();
		}

	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
