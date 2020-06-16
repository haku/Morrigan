package com.vaguehope.morrigan.util.httpclient;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HttpResponse {

	private final int responseCode;
	private final String responseBody;
	private final String etag;
	private final Map<String, List<String>> headerFields;

	public HttpResponse (final int code, final String body, final String etag, final Map<String, List<String>> headerFields) {
		this.responseCode = code;
		this.responseBody = body;
		this.etag = etag;
		this.headerFields = headerFields;
	}

	public int getCode () {
		return this.responseCode;
	}

	public String getBody () {
		return this.responseBody;
	}

	public String getEtag () {
		return this.etag;
	}

	public Map<String, List<String>> getHeaderFields () {
		return this.headerFields;
	}

	public List<String> getHeader(final String name) {
		for (Entry<String, List<String>> e : this.headerFields.entrySet()) {
			if (name.equalsIgnoreCase(e.getKey())) {
				return e.getValue();
			}
		}
		return null;
	}

	@Override
	public String toString () {
		return this.responseCode + ": " + this.responseBody;
	}

}
