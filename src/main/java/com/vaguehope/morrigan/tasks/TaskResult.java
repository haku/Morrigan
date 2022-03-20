package com.vaguehope.morrigan.tasks;

public class TaskResult {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final TaskOutcome outcome;
	private final String errMsg;
	private final Throwable errThr;

	public TaskResult (final TaskOutcome outcome) {
		this(outcome, null, null);
	}

	public TaskResult (final TaskOutcome outcome, final String errMsg, final Throwable errThr) {
		if (outcome == null) throw new IllegalArgumentException("Outcome can not be null.");
		this.outcome = outcome;
		this.errMsg = errMsg;
		this.errThr = errThr;
	}

	public TaskOutcome getOutcome() {
		return this.outcome;
	}

	public String getErrMsg() {
		return this.errMsg;
	}

	public Throwable getErrThr() {
		return this.errThr;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
