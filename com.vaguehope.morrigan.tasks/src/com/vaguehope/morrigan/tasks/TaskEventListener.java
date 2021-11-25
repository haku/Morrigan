package com.vaguehope.morrigan.tasks;

public interface TaskEventListener {

	/**
	 * Tasks reporting to TaskEventListener will always by calling this before
	 * any other method on this interface.
	 */
	void onStart ();

	/**
	 * Report a message to the user.  This will form part of the task's output
	 * report.
	 */
	void logMsg (String topic, String s);

	/**
	 * Like logMsg, but for throwables.
	 */
	void logError (String topic, String s, Throwable t);

	/**
	 * Set the name if there is a bunch of work to do before beginTask() can be called.
	 */
	void setName (String name);

	/**
	 * Called when actual work of a know size is begun.
	 * Will only be called once.
	 */
	void beginTask (String name, int totalWork);

	/**
	 * Called at the beginning of each part of a task.
	 */
	void subTask (String name);

	/**
	 * Called upon cessation of the task, weather erroneous or successful.
	 */
	void done ();

	/**
	 * Consuming task will periodically check this method to see if work
	 * should continue.  Returning true here will abort the task at the
	 * next available opportunity.
	 */
	boolean isCanceled ();

	/**
	 * Called by task to report progress.  Calls are cumulative.
	 * Total will be that reported to beginTask().
	 */
	void worked (int work);

}
