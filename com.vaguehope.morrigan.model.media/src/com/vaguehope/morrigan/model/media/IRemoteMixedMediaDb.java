package com.vaguehope.morrigan.model.media;

import java.net.URL;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.sqlitewrapper.DbException;


public interface IRemoteMixedMediaDb extends IMixedMediaDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TYPE = "REMOTEMMDB";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public URL getUrl() throws DbException;
	public void setUrl(URL url) throws DbException;
	
	public String getPass () throws DbException;
	public void setPass(String pass) throws DbException;
	
	/**
	 * Return age of cache in milliseconds.
	 */
	public long getCacheAge ();
	
	public boolean isCacheExpired ();
	public void readFromCache ()  throws DbException, MorriganException;
	public void forceDoRead () throws MorriganException, DbException;
	
	public TaskEventListener getTaskEventListener();
	public void setTaskEventListener(TaskEventListener taskEventListener);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
