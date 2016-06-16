package com.vaguehope.morrigan.tasks;

public interface AsyncTask {

	String id();
	String title();
	TaskState state();
	String subtask();
	String lastMsg();
	String lastErr();
	int progressWorked();
	int progressTotal();
	/**
	 * Null for incomplete.
	 */
	Boolean successful();
	String summary ();

}
