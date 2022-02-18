package com.vaguehope.morrigan.tasks;

public interface MorriganTask {

	String getTitle ();

	TaskResult run (TaskEventListener taskEventListener);

}
