package net.sparktank.morrigan.model;


public interface IMorriganTask {
	
	public String getTitle ();
	
	public TaskResult run (TaskEventListener taskEventListener);
	
}
