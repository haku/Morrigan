package com.vaguehope.morrigan.gui.jobs;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.vaguehope.morrigan.gui.dialogs.RunnableDialog;
import com.vaguehope.morrigan.model.tasks.IMorriganTask;
import com.vaguehope.morrigan.model.tasks.TaskResult;
import com.vaguehope.morrigan.model.tasks.TaskResult.TaskOutcome;

public class TaskJob extends Job {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final IMorriganTask task;
	
	public TaskJob (IMorriganTask task) {
		super(task.getTitle());
		this.task = task;
		setUser(true);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		TaskResult res = this.task.run(new PrgListener(monitor));
		
		if (res.getOutcome() == TaskOutcome.SUCCESS) {
			return Status.OK_STATUS;
		}
		else if (res.getOutcome() == TaskOutcome.CANCELED) {
			return Status.CANCEL_STATUS;
		}
		else {
			Display.getDefault().asyncExec(new RunnableDialog(res.getErrThr()));
			return new FailStatus(res.getErrMsg(), res.getErrThr());
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}