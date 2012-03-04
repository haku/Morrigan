package com.vaguehope.morrigan.tasks;

public interface IMorriganTask {

	String getTitle ();

	TaskResult run (TaskEventListener taskEventListener);

}
