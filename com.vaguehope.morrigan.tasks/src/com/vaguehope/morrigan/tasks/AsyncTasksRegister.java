package com.vaguehope.morrigan.tasks;

public interface AsyncTasksRegister {

	void scheduleTask (MorriganTask task);

	String reportSummary ();

	String[] reportIndiviually ();

}
