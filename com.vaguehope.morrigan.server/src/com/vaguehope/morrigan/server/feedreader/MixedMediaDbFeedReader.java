package com.vaguehope.morrigan.server.feedreader;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jetty.util.B64Code;
import org.xml.sax.SAXException;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.server.MlistsServlet;
import com.vaguehope.morrigan.server.model.RemoteMixedMediaDbFactory;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.util.httpclient.Http;
import com.vaguehope.morrigan.util.httpclient.HttpClient;
import com.vaguehope.morrigan.util.httpclient.HttpResponse;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandler;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandlerException;
import com.vaguehope.sqlitewrapper.DbException;

public class MixedMediaDbFeedReader implements HttpStreamHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String USERNAME = "Morrigan-GUI";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	/**
	 * TODO report progress to taskEventListener.
	 * TODO support event cancellation.
	 */
	public static void read (final IRemoteMixedMediaDb mmdb, final TaskEventListener taskEventListener) throws MorriganException {
		if (taskEventListener != null) taskEventListener.onStart();
		if (taskEventListener != null) taskEventListener.beginTask("Updating " + mmdb.getListName(), 100);

		try {
			final String surl = mmdb.getUrl().toString() + "/" + MlistsServlet.PATH_ITEMS + "?" + MlistsServlet.PARAM_INCLUDE_DELETED_TAGS + "=true";
			URL url = new URL(surl);
			Map<String, String> headers = new HashMap<String, String>();
			addAuthHeader(headers, USERNAME, mmdb.getPass());
			HttpResponse response = HttpClient.doHttpRequest(url, headers, new MixedMediaDbFeedReader(mmdb, taskEventListener));
			if (response.getCode() != 200) {
				throw new MorriganException("After fetching remote MMDB response code was " + response.getCode() + " (expected 200).");
			}
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

		if (taskEventListener!=null) taskEventListener.done();
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
				SAXParserFactory factory = SAXParserFactory.newInstance();
				factory.setNamespaceAware(true);
				factory.setValidating(true);
				SAXParser parser = factory.newSAXParser();
				parser.parse(is, new MixedMediaDbFeedParser(transClone, this.taskEventListener));
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

	private static void addAuthHeader (final Map<String, String> headers, final String user, final String pass) {
		headers.put(Http.HEADER_AUTHORISATION, Http.HEADER_AUTHORISATION_PREFIX + B64Code.encode(user + ":" + pass));
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
