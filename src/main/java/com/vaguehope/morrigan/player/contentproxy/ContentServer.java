package com.vaguehope.morrigan.player.contentproxy;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ContentServer {
	void doGet(HttpServletRequest req, HttpServletResponse resp, String listId, String itemId) throws IOException;
}