package com.vaguehope.morrigan.server;

import com.vaguehope.morrigan.model.tasks.IMorriganTask;
import com.vaguehope.morrigan.model.tasks.TaskEventListener;
import com.vaguehope.morrigan.util.ErrorHelper;

import net.sparktank.morrigan.model.media.IAbstractMixedMediaDb;
import net.sparktank.morrigan.model.media.ILocalMixedMediaDb;
import net.sparktank.morrigan.model.media.IRemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.impl.MediaFactoryImpl;

// TODO move this class somewhere more appropriate ???
public class HeadlessHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public boolean scheduleMmdbScan (final IAbstractMixedMediaDb<?> mmdb) {
		if (mmdb instanceof ILocalMixedMediaDb) {
			ILocalMixedMediaDb lmmdb = (ILocalMixedMediaDb) mmdb;
			return scheduleMmdbScan(lmmdb);
		}
		else if (mmdb instanceof IRemoteMixedMediaDb) {
			IRemoteMixedMediaDb rmmdb = (IRemoteMixedMediaDb) mmdb;
			return scheduleRemoteMmdbScan(rmmdb);
		}
		else {
			throw new IllegalArgumentException("Unknown type: '"+mmdb.getClass().getName()+"'.");
		}
	}
	
	static public boolean scheduleMmdbScan (final ILocalMixedMediaDb mmdb) {
		final IMorriganTask task = MediaFactoryImpl.get().getLocalMixedMediaDbUpdateTask(mmdb);
		if (task != null) {
			Thread t = new Thread () {
				@Override
				public void run() {
					task.run(new DbScanMon(mmdb.getListName()));
				}
			};
			t.start();
			System.err.println("Scan of " + mmdb.getListId() + " scheduled on thread " + t.getId() + ".");
			return true;
			
		}
		
		System.err.println("Failed to get task object from factory method.");
		return false;
	}
	
	static public boolean scheduleRemoteMmdbScan (final IRemoteMixedMediaDb mmdb) {
		final IMorriganTask task = MediaFactoryImpl.get().getRemoteMixedMediaDbUpdateTask(mmdb);
		if (task != null) {
			Thread t = new Thread () {
				@Override
				public void run() {
					task.run(new DbScanMon(mmdb.getListName()));
				}
			};
			t.start();
			System.err.println("Refresh of " + mmdb.getListId() + " scheduled on thread " + t.getId() + ".");
			return true;
			
		}
		
		System.err.println("Failed to get task object from factory method.");
		return false;
	}
	
	static class DbScanMon implements TaskEventListener {
		
		private final String logPrefix;
		private int totalWork = 0;
		private int workDone = 0;
		private boolean canceled;
		
		public DbScanMon (String logPrefix) {
			this.logPrefix = logPrefix;
		}
		
		@Override
		public void logMsg(String topic, String s) {
			System.out.print("[");
			System.out.print(topic);
			System.out.print("] ");
			System.out.print(s);
			System.out.println();
		}
		
		@Override
		public void logError(String topic, String s, Throwable t) {
			String causeTrace = ErrorHelper.getCauseTrace(t);
			logMsg(topic, s.concat("\n".concat(causeTrace)));
		}
		
		@Override
		public void onStart() {/* UNUSED */}
		
		@Override
		public void beginTask(String name, @SuppressWarnings("hiding") int totalWork) {
			this.totalWork = totalWork;
			System.out.println("[" + this.logPrefix + "] starting task: " + name + ".");
		}
		
		@Override
		public void done() {
			System.out.println("[" + this.logPrefix + "] done.");
		}
		
		@Override
		public void subTask(String name) {
			System.out.println("[" + this.logPrefix + "] sub task: "+name+".");
		}
		
		@Override
		public boolean isCanceled() {
			return this.canceled;
		}
		
		@Override
		public void worked(int work) {
			this.workDone = this.workDone + work;
			System.out.println("[" + this.logPrefix + "] worked " + this.workDone + " of " + this.totalWork + ".");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
