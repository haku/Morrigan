package com.vaguehope.morrigan.server.feedreader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.server.MlistsServlet;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbFactory;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskOutcome;
import com.vaguehope.morrigan.util.httpclient.HttpClient;
import com.vaguehope.morrigan.util.httpclient.HttpResponse;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandler;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandlerException;

public class MixedMediaDbFeedReader implements HttpStreamHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * TODO report progress to taskEventListener.
	 * TODO support event cancellation.
	 */
	public static void read (final IRemoteMixedMediaDb mmdb, final TaskEventListener taskEventListener) throws MorriganException {
		if (taskEventListener != null) taskEventListener.onStart();
		if (taskEventListener != null) taskEventListener.beginTask("Updating " + mmdb.getListName(), 100);

		try {
			final URI baseUri = mmdb.getUri();
			final HttpStreamHandler feedReader = new MixedMediaDbFeedReader(mmdb, taskEventListener);

			if ("http".equalsIgnoreCase(baseUri.getScheme())) {
				final URI uri = new URI(baseUri + "/" + MlistsServlet.PATH_ITEMS
						+ "?" + MlistsServlet.PARAM_INCLUDE_DELETED_TAGS + "=true");
				readHttp(mmdb, uri, feedReader);
			}
			else if ("file".equalsIgnoreCase(baseUri.getScheme())) {
				readFile(baseUri, feedReader);
			}
			else {
				throw new MorriganException("Unsuported scheme: " + baseUri);
			}
		}
		catch (final URISyntaxException e) {
			throw new MorriganException("Invalid URI: " + e.getInput(), e);
		}
		catch (IOException e) {
			if (e instanceof UnknownHostException) {
				throw new MorriganException("Host unknown.", e);
			} else if (e instanceof SocketException) {
				throw new MorriganException("Host unreachable.", e);
			} else {
				throw new MorriganException(e);
			}
		} catch (HttpStreamHandlerException e) {
			throw new MorriganException(e);
		} catch (DbException e) {
			throw new MorriganException(e);
		}

		if (taskEventListener != null) taskEventListener.done(TaskOutcome.SUCCESS);
	}

	private static void readHttp (final IRemoteMixedMediaDb mmdb, final URI uri, final HttpStreamHandler feedReader) throws DbException, IOException, HttpStreamHandlerException, MalformedURLException, MorriganException {
		final Map<String, String> headers = new HashMap<String, String>();
		Auth.addTo(headers, uri, mmdb.getPass());
		final HttpResponse response = HttpClient.doHttpRequest(uri.toURL(), headers, feedReader);
		if (response.getCode() != 200) {
			throw new MorriganException("After fetching remote MMDB response code was " + response.getCode() + " (expected 200).");
		}
	}

	private static void readFile (final URI uri, final HttpStreamHandler feedReader) throws MorriganException, IOException, HttpStreamHandlerException {
		final File file;
		try {
			file = new File(uri);
		}
		catch (final IllegalArgumentException e) {
			throw new MorriganException("Failed to convert URI to File: " + uri, e);
		}
		if (!file.exists()) throw new MorriganException("File not found: " + file.getAbsolutePath());

		final InputStream is = new FileInputStream(file);
		try {
			feedReader.handleStream(new BufferedInputStream(is));
		}
		finally {
			is.close();
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final IRemoteMixedMediaDb mmdb;
	private final TaskEventListener taskEventListener;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private MixedMediaDbFeedReader (final IRemoteMixedMediaDb mmdb, final TaskEventListener taskEventListener) {
		this.mmdb = mmdb;
		this.taskEventListener = taskEventListener;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void handleStream(final InputStream is) throws IOException, HttpStreamHandlerException {
		boolean thereWereErrors = true;
		IRemoteMixedMediaDb transClone = null;
		try {
			transClone = RemoteMixedMediaDbFactory.getTransactionalClone(this.mmdb);
			transClone.setDefaultMediaType(MediaType.UNKNOWN, false);
			transClone.readFromCache();
			transClone.beginBulkUpdate();
			try {
				final InputSource inputSource = new InputSource(is);
				inputSource.setEncoding("UTF-8");

				final MixedMediaDbFeedParser handler = new MixedMediaDbFeedParser(transClone, this.taskEventListener);

				final SAXParserFactory factory = SAXParserFactory.newInstance();
				factory.setNamespaceAware(true);
				factory.setValidating(true);
				factory.newSAXParser().parse(inputSource, handler);
			}
			catch (SAXException e) {
				throw new HttpStreamHandlerException(e);
			} catch (ParserConfigurationException e) {
				throw new HttpStreamHandlerException(e);
			} catch (IOException e) {
				throw new HttpStreamHandlerException(e);
			}
			thereWereErrors = false;
		}
		catch (DbException e) {
			throw new HttpStreamHandlerException(e);
		} catch (MorriganException e) {
			throw new HttpStreamHandlerException(e);
		}
		finally {
			if (transClone != null) {
				try {
					transClone.completeBulkUpdate(thereWereErrors);
				} catch (DbException e) {
					throw new HttpStreamHandlerException(e);
				} catch (MorriganException e) {
					throw new HttpStreamHandlerException(e);
				} finally {
					try {
						if (thereWereErrors) {
							transClone.rollback();
						}
						else {
							transClone.commitOrRollback();
						}
					} catch (DbException e) {
						throw new HttpStreamHandlerException(e);
					}
					finally {
						transClone.dispose();
					}
				}
			}
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
