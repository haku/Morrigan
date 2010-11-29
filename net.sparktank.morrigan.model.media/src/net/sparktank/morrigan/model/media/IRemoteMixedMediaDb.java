package net.sparktank.morrigan.model.media;

import java.net.URL;

import net.sparktank.morrigan.model.exceptions.MorriganException;
import net.sparktank.morrigan.model.tasks.TaskEventListener;
import net.sparktank.sqlitewrapper.DbException;

public interface IRemoteMixedMediaDb extends IAbstractMixedMediaDb<IRemoteMixedMediaDb> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public URL getUrl();
	
	public long getCacheAge ();
	public boolean isCacheExpired ();
	public void readFromCache ()  throws DbException, MorriganException;
	public void forceDoRead () throws MorriganException, DbException;
	
	public TaskEventListener getTaskEventListener();
	public void setTaskEventListener(TaskEventListener taskEventListener);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
