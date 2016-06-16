package com.vaguehope.morrigan.tasks;

import java.util.Collection;

public interface AsyncTasksRegister {

	AsyncTask scheduleTask (MorriganTask task);

	String reportSummary ();

	String[] reportIndiviually ();

	Collection<AsyncTask> tasks();

}
