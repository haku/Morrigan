package com.vaguehope.morrigan.tasks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.vaguehope.morrigan.util.DaemonThreadFactory;

public class TasksActivator implements BundleActivator {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private ExecutorService executor;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void start (final BundleContext context) throws Exception {
		this.executor = Executors.newCachedThreadPool(new DaemonThreadFactory("tsk"));
		context.registerService(AsyncTasksRegister.class, new AsyncTasksRegisterImpl(this.executor), null);
	}

	@Override
	public void stop (final BundleContext context) throws Exception {
		this.executor.shutdown(); // TODO more controlled wait for tasks to be cancelled.
		this.executor = null;
	}

}
