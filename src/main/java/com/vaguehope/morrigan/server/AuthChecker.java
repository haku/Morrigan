package com.vaguehope.morrigan.server;


public interface AuthChecker {

	boolean verifyAuth (String user, String pass);

}
