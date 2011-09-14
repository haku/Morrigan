package com.vaguehope.morrigan.model.tasks;

public class TaskResult {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static public enum TaskOutcome {SUCCESS, FAILED, CANCELED}
	
	private TaskOutcome outcome;
	private String errMsg;
	private Throwable errThr;
	
	public TaskResult (TaskOutcome outcome) {
		this.outcome = outcome;
	}
	
	public TaskResult (TaskOutcome outcome, String errMsg, Throwable errThr) {
		this.outcome = outcome;
		this.errMsg = errMsg;
		this.errThr = errThr;
	}
	
	public void setOutcome(TaskOutcome outcome) {
		this.outcome = outcome;
	}
	public TaskOutcome getOutcome() {
		return this.outcome;
	}
	
	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}
	public String getErrMsg() {
		return this.errMsg;
	}
	
	public void setErrThr(Throwable errThr) {
		this.errThr = errThr;
	}
	public Throwable getErrThr() {
		return this.errThr;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
