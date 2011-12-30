package com.vaguehope.morrigan.util.httpclient;

import java.util.List;
import java.util.Map;

public class HttpResponse {
	
	private int responseCode;
	private String responseBody;
	private String etag;
	private Map<String, List<String>> headerFields;
	
	public HttpResponse (int code, String body, String etag, Map<String, List<String>> headerFields) {
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
	
	@Override
	public String toString () {
		return this.responseCode + ": " + this.responseBody;
	}
	
}
