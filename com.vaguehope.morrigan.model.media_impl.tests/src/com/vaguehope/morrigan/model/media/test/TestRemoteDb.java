package com.vaguehope.morrigan.model.media.test;

import java.net.URI;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.sqlitewrapper.DbException;

public class TestRemoteDb extends TestMixedMediaDb implements IRemoteMixedMediaDb {

	public TestRemoteDb () throws DbException, MorriganException {
		super("testRemoteDb");
	}

	@Override
	public URI getUri () throws DbException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setUri (final URI uri) throws DbException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public String getPass () throws DbException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setPass (final String pass) throws DbException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public long getCacheAge () {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public boolean isCacheExpired () {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void readFromCache () throws DbException, MorriganException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void forceDoRead () throws MorriganException, DbException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public TaskEventListener getTaskEventListener () {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public void setTaskEventListener (final TaskEventListener taskEventListener) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not implemented.");
	}

}
