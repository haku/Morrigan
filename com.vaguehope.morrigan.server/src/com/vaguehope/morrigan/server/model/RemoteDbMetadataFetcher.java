package com.vaguehope.morrigan.server.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
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

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.server.feedreader.Auth;
import com.vaguehope.morrigan.util.httpclient.HttpClient;
import com.vaguehope.morrigan.util.httpclient.HttpResponse;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandler;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandlerException;

public class RemoteDbMetadataFetcher {

	private final URI remoteUri;
	private Document doc;

	public RemoteDbMetadataFetcher (final URI remoteUri) {
		this.remoteUri = remoteUri;
	}

	public void fetch () throws IOException, HttpStreamHandlerException, MorriganException {
		final XmlParser parser = new XmlParser();

		if ("http".equalsIgnoreCase(this.remoteUri.getScheme())) {
			readHttp(parser);
		}
		else if ("file".equalsIgnoreCase(this.remoteUri.getScheme())) {
			readFile(parser);
		}
		else {
			throw new MorriganException("Unsuported scheme: " + this.remoteUri);
		}

		this.doc = parser.getDoc();
	}

	private void readHttp (final XmlParser parser) throws MalformedURLException, IOException, HttpStreamHandlerException {
		final Map<String, String> headers = new HashMap<String, String>();
		Auth.addTo(headers, this.remoteUri);
		final HttpResponse response = HttpClient.doHttpRequest(this.remoteUri.toURL(), headers, parser);
		if (response.getCode() != 200) throw new IOException("HTTP " + response.getCode() + " fetching DB metadata.");
	}

	private void readFile (final XmlParser parser) throws MorriganException, FileNotFoundException, IOException, HttpStreamHandlerException {
		final File file;
		try {
			file = new File(this.remoteUri);
		}
		catch (final IllegalArgumentException e) {
			throw new MorriganException("Failed to convert URI to File: " + this.remoteUri, e);
		}
		if (!file.exists()) throw new MorriganException("File not found: " + file.getAbsolutePath());

		final InputStream is = new FileInputStream(file);
		try {
			parser.handleStream(new BufferedInputStream(is));
		}
		finally {
			is.close();
		}
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
