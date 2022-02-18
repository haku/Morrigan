package com.vaguehope.morrigan.tasks;

public interface AsyncTask {

	/**
	 * Number since start.
	 */
	int number();

	/**
	 * UUID.
	 */
	String id();
	String title();
	TaskState state();

	void cancel();
	boolean isCanceled ();

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
