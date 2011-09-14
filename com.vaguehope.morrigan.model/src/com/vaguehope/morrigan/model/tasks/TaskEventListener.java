package com.vaguehope.morrigan.model.tasks;


// TODO move this class somewhere more sensible.
public interface TaskEventListener {
	public void onStart ();
	public void logMsg (String topic, String s);
	public void logError (String topic, String s, Throwable t);
	
	public void beginTask(String name, int totalWork);
	public void subTask(String name);
	public void done();
	public boolean isCanceled();
	public void worked(int work);
}
