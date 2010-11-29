package net.sparktank.morrigan.helpers;

import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDb;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDbUpdateTask;
import net.sparktank.morrigan.model.media.impl.RemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.impl.RemoteMixedMediaDbUpdateTask;
import net.sparktank.morrigan.model.tasks.TaskEventListener;

// TODO move this class somewhere more appropriate ???
public class HeadlessHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public boolean scheduleMmdbScan (final LocalMixedMediaDb mmdb) {
		final LocalMixedMediaDbUpdateTask task = LocalMixedMediaDbUpdateTask.FACTORY.manufacture(mmdb);
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
	
	static public boolean scheduleRemoteMmdbScan (final RemoteMixedMediaDb mmdb) {
		final RemoteMixedMediaDbUpdateTask task = RemoteMixedMediaDbUpdateTask.FACTORY.manufacture(mmdb);
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
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			sb.append(topic);
			sb.append("] ");
			sb.append(s);
			
			System.out.println(sb.toString());
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
