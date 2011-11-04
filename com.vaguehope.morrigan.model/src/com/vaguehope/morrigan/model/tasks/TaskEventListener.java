package com.vaguehope.morrigan.model.tasks;

public interface TaskEventListener {
	
	/**
	 * Tasks reporting to TaskEventListener will always by calling this before
	 * any other method on this interface.
	 */
	public void onStart ();
	
	/**
	 * Report a message to the user.  This will form part of the task's output
	 * report.
	 */
	public void logMsg (String topic, String s);
	
	/**
	 * Like logMsg, but for throwables.
	 */
	public void logError (String topic, String s, Throwable t);
	
	/**
	 * Called when actual work of a know size is begun.
	 * Will only be called once.
	 */
	public void beginTask (String name, int totalWork);
	
	/**
	 * Called at the beginning of each part of a task.
	 */
	public void subTask (String name);
	
	/**
	 * Called upon cessation of the task, weather erroneous or successful.
	 */
	public void done ();
	
	/**
	 * Consuming task will periodically check this method to see if work
	 * should continue.  Returning true here will abort the task at the
	 * next available opportunity.
	 */
	public boolean isCanceled ();
	
	/**
	 * Called by task to report progress.  Calls are cumulative.
	 * Total will be that reported to beginTask().
	 */
	public void worked (int work);
	
}
