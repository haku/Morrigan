package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FixtureServlet extends HttpServlet {

	private static final long serialVersionUID = 1164486017828356643L;
	private final Map<String, Fixture> getFixtures = new ConcurrentHashMap<String, Fixture>();

	public void addGetFixture (final URI uri, final String contentType, final String body) {
		String path = uri.getPath();
		if (uri.getQuery() != null) path += "?" + uri.getQuery();
		addGetFixture(path, contentType, body);
	}

	public void addGetFixture (final String path, final String contentType, final String body) {
		this.getFixtures.put(path, new Fixture(contentType, body));
		System.out.println("Fxiture added: " + path);
	}

	@Override
	protected void doGet (final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		String url = req.getRequestURI();
		final String query = req.getQueryString();
		if (query != null) url += "?" + query;

		final Fixture fixture = this.getFixtures.get(url);
		if (fixture == null) {
			System.out.println("Fixture not found: " + url);
			ServletHelper.error(resp, 404, "Fixture not found: " + url);
			return;
		}

		resp.setContentType(fixture.contentType);
		resp.getWriter().write(fixture.body);
		resp.flushBuffer();
	}

	private class Fixture {

		final String contentType;
		final String body;

		public Fixture (final String contentType, final String body) {
			this.contentType = contentType;
			this.body = body;
		}

	}

}
