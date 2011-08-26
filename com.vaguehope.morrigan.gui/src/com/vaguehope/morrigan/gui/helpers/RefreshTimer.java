package com.vaguehope.morrigan.gui.helpers;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.swt.widgets.Display;

public class RefreshTimer implements Runnable {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	final Display display;
	final int minInterval;
	final Runnable task;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public RefreshTimer (Display display, int minInterval, Runnable task) {
		this.display = display;
		this.minInterval = minInterval;
		this.task = task;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void scheduleUpdate () {
		this.update(false);
	}
	
//	public void updateNow () {
//		this.update(true);
//	}
	
	public void reset () {
		this.lastRun.addAndGet(-this.minInterval); // Push it back in time.
	}
	
	@Override
	public void run() { // from Runnable interface.
		scheduleUpdate();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	protected AtomicBoolean running = new AtomicBoolean(false);
	protected AtomicBoolean requested = new AtomicBoolean(false);
	protected AtomicLong lastRun = new AtomicLong();
	
	private void update (boolean force) {
		if (this.requested.compareAndSet(false, true) && !this.running.get()) {
			final long d = System.currentTimeMillis() - this.lastRun.get();
			if (force || d > this.minInterval) {
//				System.err.println("RefreshTimer.triggerUpdate(): calling asyncExec(update).");
				this.display.asyncExec(this.update);
			}
			else {
				this.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						int w = (int) (RefreshTimer.this.minInterval - d);
//						System.err.println("RefreshTimer.triggerUpdate(): calling display.timerExec("+w+",update).");
						RefreshTimer.this.display.timerExec(w, RefreshTimer.this.update);
					}
				});
			}
		}
	}
	
	protected Runnable update = new Runnable() {
		@Override
		public void run() {
			RefreshTimer.this.running.set(true);
			RefreshTimer.this.requested.set(false);
			
//			System.err.println("RefreshTimer.update.run(): calling task.run().");
			RefreshTimer.this.task.run();
			
			RefreshTimer.this.lastRun.set(System.currentTimeMillis());
			
			if (RefreshTimer.this.requested.get()) {
//				System.err.println("RefreshTimer.update.run(): calling display.timerExec("+RefreshTimer.this.minInterval+",update).");
				RefreshTimer.this.display.timerExec(RefreshTimer.this.minInterval, this);
			}
			else {
				RefreshTimer.this.running.set(false);
			}
		}
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
