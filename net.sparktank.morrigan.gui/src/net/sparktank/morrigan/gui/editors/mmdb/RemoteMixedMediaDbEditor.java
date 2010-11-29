package net.sparktank.morrigan.gui.editors.mmdb;

import java.util.List;

import net.sparktank.morrigan.gui.actions.CopyToLocalMmdbAction;
import net.sparktank.morrigan.gui.dialogs.MorriganMsgDlg;
import net.sparktank.morrigan.gui.jobs.TaskJob;
import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.IMixedMediaItem;
import net.sparktank.morrigan.model.media.impl.RemoteMixedMediaDb;
import net.sparktank.morrigan.model.media.impl.RemoteMixedMediaDbUpdateTask;
import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorReference;

public class RemoteMixedMediaDbEditor
		extends AbstractMixedMediaDbEditor<RemoteMixedMediaDb> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.gui.editors.RemoteMixedMediaDbEditor";
	
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
			RemoteMixedMediaDbUpdateTask task = RemoteMixedMediaDbUpdateTask.FACTORY.manufacture(getMediaList());
			if (task != null) {
				TaskJob job = new TaskJob(task, getSite().getShell().getDisplay());
				job.schedule(3000);
			}
			else {
				new MorriganMsgDlg("An update is already running for this library.").open();
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Actions and listeners.
	
	private SelectionAdapter refreshListener = new SelectionAdapter() {
		@Override
		public void widgetSelected(SelectionEvent event) {
			try {
				RemoteMixedMediaDbUpdateTask task = RemoteMixedMediaDbUpdateTask.FACTORY.manufacture(getMediaList());
				if (task != null) {
					TaskJob job = new TaskJob(task, getSite().getShell().getDisplay());
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
