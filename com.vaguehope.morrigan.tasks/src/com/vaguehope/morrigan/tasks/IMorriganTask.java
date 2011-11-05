package com.vaguehope.morrigan.tasks;



public interface IMorriganTask {
	
	public String getTitle ();
	
	public TaskResult run (TaskEventListener taskEventListener);
	
}
