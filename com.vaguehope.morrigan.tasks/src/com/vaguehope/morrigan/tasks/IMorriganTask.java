package com.vaguehope.morrigan.tasks;

// TODO rename to MorriganTask.
public interface IMorriganTask {

	String getTitle ();

	TaskResult run (TaskEventListener taskEventListener);

}
