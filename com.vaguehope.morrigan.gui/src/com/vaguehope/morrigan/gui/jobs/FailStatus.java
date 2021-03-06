package com.vaguehope.morrigan.gui.jobs;

import org.eclipse.core.runtime.IStatus;

public class FailStatus implements IStatus {
	
	private final String message;
	private final Throwable e;

	public FailStatus (String message, Throwable e) {
		this.message = message;
		this.e = e;
	}
	
	@Override
	public IStatus[] getChildren() {
		return null;
	}
	
	@Override
	public int getCode() {
		return -1;
	}
	
	@Override
	public Throwable getException() {
		return this.e;
	}
	
	@Override
	public String getMessage() {
		return this.message;
	}
	
	@Override
	public String getPlugin() {
		return "com.vaguehope.morrigan";
	}
	
	@Override
	public int getSeverity() {
		return ERROR;
	}
	
	@Override
	public boolean isMultiStatus() {
		return false;
	}
	
	@Override
	public boolean isOK() {
		return false;
	}
	
	@Override
	public boolean matches(int severityMask) {
		return false;
	}
	
}
