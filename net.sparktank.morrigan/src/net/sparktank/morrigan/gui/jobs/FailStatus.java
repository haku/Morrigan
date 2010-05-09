package net.sparktank.morrigan.gui.jobs;

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
		return 0;
	}
	
	@Override
	public Throwable getException() {
		return e;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
	
	@Override
	public String getPlugin() {
		return null;
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
