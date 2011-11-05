package com.vaguehope.morrigan.server;

import com.vaguehope.morrigan.model.media.IMixedMediaDb;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.impl.MediaFactoryImpl;
import com.vaguehope.morrigan.tasks.AsyncProgressRegister;
import com.vaguehope.morrigan.tasks.IMorriganTask;

/**
 * Set of helper functions for interacting
 * when there is no GUI.
 *
 */
public class AsyncActions {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public void scheduleMmdbScan (final IMixedMediaDb mmdb) {
		if (mmdb instanceof ILocalMixedMediaDb) {
			ILocalMixedMediaDb lmmdb = (ILocalMixedMediaDb) mmdb;
			scheduleMmdbScan(lmmdb);
		}
		else if (mmdb instanceof IRemoteMixedMediaDb) {
			IRemoteMixedMediaDb rmmdb = (IRemoteMixedMediaDb) mmdb;
			scheduleRemoteMmdbScan(rmmdb);
		}
		else {
			throw new IllegalArgumentException("Unknown type: '"+mmdb.getClass().getName()+"'.");
		}
	}
	
	static public void scheduleMmdbScan (final ILocalMixedMediaDb mmdb) {
		final IMorriganTask task = MediaFactoryImpl.get().getLocalMixedMediaDbUpdateTask(mmdb);
		if (task != null) {
			Thread t = new Thread () {
				@Override
				public void run() {
					task.run(AsyncProgressRegister.makeTrackedListener());
				}
			};
			t.start();
			return;
		}
		throw new IllegalArgumentException("Failed to get task object from factory method.");
	}
	
	static public void scheduleRemoteMmdbScan (final IRemoteMixedMediaDb mmdb) {
		final IMorriganTask task = MediaFactoryImpl.get().getRemoteMixedMediaDbUpdateTask(mmdb);
		if (task != null) {
			Thread t = new Thread () {
				@Override
				public void run() {
					task.run(AsyncProgressRegister.makeTrackedListener());
				}
			};
			t.start();
			return;
			
		}
		throw new IllegalArgumentException("Failed to get task object from factory method.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
