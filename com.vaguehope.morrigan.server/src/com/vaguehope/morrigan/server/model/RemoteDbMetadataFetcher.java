package com.vaguehope.morrigan.server.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.vaguehope.morrigan.server.feedreader.Auth;
import com.vaguehope.morrigan.util.httpclient.HttpClient;
import com.vaguehope.morrigan.util.httpclient.HttpResponse;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandler;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandlerException;

public class RemoteDbMetadataFetcher {

	private final String remoteUrl;
	private Document doc;

	public RemoteDbMetadataFetcher (final String remoteUrl) {
		this.remoteUrl = remoteUrl;
	}

	public void fetch () throws IOException, HttpStreamHandlerException {
		final URL url = new URL(this.remoteUrl);
		final XmlParser parser = new XmlParser();
		final Map<String, String> headers = new HashMap<String, String>();
		Auth.addTo(headers, url);
		final HttpResponse response = HttpClient.doHttpRequest(url, headers, parser);
		if (response.getCode() != 200) throw new IOException("HTTP " + response.getCode() + " fetching DB metadata.");
		this.doc = parser.getDoc();
	}

	public UUID getUuid() {
		if (this.doc == null) throw new IllegalStateException();
		final NodeList uuidNodes = this.doc.getElementsByTagName("uuid");
		if (uuidNodes.getLength() > 0) {
			return UUID.fromString(uuidNodes.item(0).getTextContent());
		}
		throw new IllegalStateException("UUID mising from DB response document.");
	}

	private final class XmlParser implements HttpStreamHandler {

		private Document doc;

		public Document getDoc () {
			return this.doc;
		}

		@Override
		public void handleStream (final InputStream is) throws IOException, HttpStreamHandlerException {
			try {
				final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				final DocumentBuilder builder = factory.newDocumentBuilder();
				this.doc = builder.parse(new InputSource(is));
			}
			catch (final ParserConfigurationException e) {
				throw new IllegalStateException(e);
			}
			catch (final SAXException e) {
				throw new HttpStreamHandlerException(e.toString(), e);
			}
		}

	}

}
