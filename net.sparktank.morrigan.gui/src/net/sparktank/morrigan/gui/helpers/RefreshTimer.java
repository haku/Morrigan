package net.sparktank.morrigan.gui.helpers;

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
	
	AtomicBoolean running = new AtomicBoolean(false);
	AtomicBoolean requested = new AtomicBoolean(false);
	AtomicLong lastRun = new AtomicLong();
	
	public void triggerUpdate () {
		if (this.requested.compareAndSet(false, true) && !this.running.get()) {
			final long d = System.currentTimeMillis() - this.lastRun.get();
			if (d > this.minInterval) {
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
	
	Runnable update = new Runnable() {
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
	
	@Override
	public void run() {
		triggerUpdate();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
