package com.vaguehope.morrigan.model;

public interface Register<T> {

	int nextIndex ();
	void register (T target);
	void unregister (T target);

}
