package com.vaguehope.morrigan.tasks;

import java.util.List;

public interface AsyncTasksRegister {

	AsyncTask scheduleTask (MorriganTask task);

	String reportSummary ();

	String[] reportIndiviually ();

	List<AsyncTask> tasks();
	AsyncTask task(String id);

}
