package com.vaguehope.morrigan.model.media;

import java.net.URL;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.sqlitewrapper.DbException;


public interface IRemoteMixedMediaDb extends IMixedMediaDb {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	String TYPE = "REMOTEMMDB";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	URL getUrl() throws DbException;
	void setUrl(URL url) throws DbException;

	String getPass () throws DbException;
	void setPass(String pass) throws DbException;

	/**
	 * Return age of cache in milliseconds.
	 */
	long getCacheAge ();

	boolean isCacheExpired ();
	void readFromCache ()  throws DbException, MorriganException;
	void forceDoRead () throws MorriganException, DbException;

	TaskEventListener getTaskEventListener();
	void setTaskEventListener(TaskEventListener taskEventListener);

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
