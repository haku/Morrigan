package com.vaguehope.morrigan.model;

public interface Register<T> {

	String nextIndex (String prefix);
	void register (T target);
	void unregister (T target);

}
