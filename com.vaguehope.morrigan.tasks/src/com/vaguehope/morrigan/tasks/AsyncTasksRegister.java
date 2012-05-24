package com.vaguehope.morrigan.tasks;

public interface AsyncTasksRegister {

	void scheduleTask (IMorriganTask task);

	String reportSummary ();

	String[] reportIndiviually ();

}
