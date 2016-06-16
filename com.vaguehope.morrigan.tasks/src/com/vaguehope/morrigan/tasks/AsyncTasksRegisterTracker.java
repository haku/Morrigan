package com.vaguehope.morrigan.tasks;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class AsyncTasksRegisterTracker implements AsyncTasksRegister {

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final ServiceTracker<AsyncTasksRegister, AsyncTasksRegister> tracker;

	public AsyncTasksRegisterTracker (final BundleContext context) {
		this.tracker = new ServiceTracker<AsyncTasksRegister, AsyncTasksRegister>(context, AsyncTasksRegister.class, null);
		this.tracker.open();
	}

	public void dispose () {
		this.alive.set(false);
		this.tracker.close();
	}

	private void checkAlive () {
		if (!this.alive.get()) throw new IllegalStateException(this.getClass().getName() + " is disposed.");
	}

	private AsyncTasksRegister getServiceOptional () {
		checkAlive();
		AsyncTasksRegister service = this.tracker.getService();
		return service;
	}

	private AsyncTasksRegister getService () {
		AsyncTasksRegister service = getServiceOptional();
		if (service == null) throw new IllegalStateException("AsyncTasksRegister service not available.");
		return service;
	}

	@Override
	public AsyncTask scheduleTask (final MorriganTask task) {
		return getService().scheduleTask(task);
	}

	@Override
	public String reportSummary () {
		AsyncTasksRegister service = getServiceOptional();
		return service == null ? "No tasks to report.\n" : service.reportSummary();
	}

	@Override
	public String[] reportIndiviually () {
		AsyncTasksRegister service = getServiceOptional();
		return service == null ? new String[]{} : service.reportIndiviually();
	}

	@Override
	public Collection<AsyncTask> tasks () {
		final AsyncTasksRegister service = getServiceOptional();
		return service == null ? Collections.<AsyncTask>emptySet() : service.tasks();
	}

}
