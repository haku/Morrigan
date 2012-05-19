package com.vaguehope.morrigan.gui.editors.mmdb;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorReference;

import com.vaguehope.morrigan.gui.Activator;
import com.vaguehope.morrigan.gui.actions.CopyToLocalMmdbAction;
import com.vaguehope.morrigan.gui.dialogs.MorriganMsgDlg;
import com.vaguehope.morrigan.gui.jobs.TaskJob;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.tasks.IMorriganTask;
import com.vaguehope.sqlitewrapper.DbException;

public class RemoteMixedMediaDbEditor
		extends AbstractMixedMediaDbEditor<IRemoteMixedMediaDb> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String ID = "com.vaguehope.morrigan.gui.editors.RemoteMixedMediaDbEditor";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Create GUI.

	@Override
	protected List<Control> populateToolbar(Composite parent) {
		List<Control> ret = super.populateToolbar(parent);

		Button btnRefresh = new Button(parent, SWT.PUSH);
		btnRefresh.setText("Refresh");
		btnRefresh.addSelectionListener(this.refreshListener);
		ret.add(ret.size() - 1, btnRefresh);

		return ret;
	}

	@Override
	protected void populateContextMenu(List<IContributionItem> menu0, List<IContributionItem> menu1) {
		menu0.add(getCopyToLocalMenu());
		super.populateContextMenu(menu0, menu1);
	}

	protected MenuManager getCopyToLocalMenu () {
		final MenuManager menu = new MenuManager("Copy to local DB...");
		menu.addMenuListener(new IMenuListener () {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				IEditorReference[] editors = getEditorSite().getPage().getEditorReferences();
				for (final IEditorReference e : editors) {
					if (e.getId().equals(LocalMixedMediaDbEditor.ID)) {
						CopyToLocalMmdbAction<IMixedMediaItem> a = new CopyToLocalMmdbAction<IMixedMediaItem>(RemoteMixedMediaDbEditor.this, e);
						menu.add(a);
					}
				}
				if (menu.getItems().length < 1) {
					Action a = new Action("(No local DBs open)") {/* UNUSED */};
					a.setEnabled(false);
					menu.add(a);
				}
			}
		});

		menu.setRemoveAllWhenShown(true);

		return menu;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Events.

	@Override
	protected boolean handleReadError(Exception e) {
		new MorriganMsgDlg(e).open();
		return true;
	}

	@Override
	protected void readInputData() throws MorriganException {
		try {
			getMediaList().readFromCache();
		} catch (DbException e) {
			throw new MorriganException(e);
		}

		if (getMediaList().isCacheExpired()) {
			IMorriganTask task = Activator.getMediaFactory().getRemoteMixedMediaDbUpdateTask(getMediaList());
			if (task != null) {
				TaskJob job = new TaskJob(task);
				job.schedule(3000);
			}
			else {
				new MorriganMsgDlg("An update is already running for this library.").open();
			}
		}
	}

	@Override
	protected void middleClickEvent (MouseEvent e) {
		// Unused.
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions and listeners.

	private SelectionAdapter refreshListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			try {
				IMorriganTask task = Activator.getMediaFactory().getRemoteMixedMediaDbUpdateTask(getMediaList());
				if (task != null) {
					TaskJob job = new TaskJob(task);
					job.schedule();
				}
				else {
					new MorriganMsgDlg("Refresh for '"+getMediaList().getListName()+"' already running.").open();
				}

			} catch (Exception e) {
				new MorriganMsgDlg(e).open();
			}
		}
	};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
