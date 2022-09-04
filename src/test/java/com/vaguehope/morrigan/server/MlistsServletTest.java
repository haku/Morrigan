package com.vaguehope.morrigan.server;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.internal.MediaFactoryImpl;
import com.vaguehope.morrigan.model.media.test.TestMixedMediaDb;
import com.vaguehope.morrigan.player.PlayerReader;
import com.vaguehope.morrigan.player.test.MockPlayerReader;
import com.vaguehope.morrigan.tasks.AsyncTasksRegister;
import com.vaguehope.morrigan.tasks.AsyncTasksRegisterImpl;
import com.vaguehope.morrigan.transcode.Transcoder;
import com.vaguehope.morrigan.util.DaemonThreadFactory;
import com.vaguehope.morrigan.util.IoHelper;

public class MlistsServletTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private Config config;
	private PlayerReader playerReader;
	private MediaFactoryImpl mediaFactory;
	private ExecutorService executor;
	private AsyncTasksRegister asyncTasksRegister;
	private AsyncActions asyncActions;
	private Transcoder transcoder;

	private MlistsServlet undertest;

	private TestMixedMediaDb testDb;
	private MockHttpServletRequest req;
	private MockHttpServletResponse resp;

	@Before
	public void before () throws Exception {
		this.config = new Config(this.tmp.getRoot());
		this.playerReader = new MockPlayerReader();
		this.mediaFactory = new MediaFactoryImpl(this.config, null);
		this.executor = Executors.newCachedThreadPool(new DaemonThreadFactory("test"));
		this.asyncTasksRegister = new AsyncTasksRegisterImpl(this.executor);
		this.asyncActions = new AsyncActions(this.asyncTasksRegister, this.mediaFactory, this.config);
		this.transcoder = new Transcoder("test");
		this.undertest = new MlistsServlet(this.playerReader, this.mediaFactory, this.asyncActions, this.transcoder, this.config);

		this.req = new MockHttpServletRequest();
		this.resp = new MockHttpServletResponse();

		this.testDb = new TestMixedMediaDb("server-test-db");
		this.mediaFactory.addLocalMixedMediaDb(this.testDb);
	}

	@Test
	public void itServesRootList () throws Exception {
		this.req.requestURI = "/mlists";
		this.undertest.service(this.req, this.resp);

		final String expected = IoHelper.readAsString(getClass().getResourceAsStream("/mlists.xml"));
		assertEquals(expected, this.resp.getOutputAsString());
	}

	@Test
	public void itServesSavedViewsNoFile () throws Exception {
		this.req.requestURI = "/mlists/savedviews";
		this.undertest.service(this.req, this.resp);
		assertEquals("[]", this.resp.getOutputAsString());
	}

	@Test
	public void itServesSavedViewsFile () throws Exception {
		this.req.requestURI = "/mlists/savedviews";

		final String expected = IoHelper.readAsString(getClass().getResourceAsStream("/savedviews.json"));
		IoHelper.write(expected, this.tmp.newFile("savedviews.json"));

		this.undertest.service(this.req, this.resp);
		assertEquals(expected, this.resp.getOutputAsString());
		assertEquals("text/json;charset=utf-8", this.resp.contentType);
	}

	@Test
	public void itDoesNotIncludeDeletedTagsIfNotRequested () throws Exception {
		// TODO
	}

	@Test
	public void itIncludesDeletedTagsIfRequested () throws Exception {
		// TODO!
	}

	@Test
	public void itServesSha1Tags() throws Exception {
		final IMixedMediaItem t0 = this.testDb.addTestTrack(BigInteger.ZERO, BigInteger.valueOf(0x1234567890abcdefL));
		this.testDb.addTag(t0, "foo", MediaTagType.MANUAL, (String) null);
		this.testDb.addTag(t0, "bar", MediaTagType.MANUAL, (String) null);
		this.testDb.addTag(t0, "bat", MediaTagType.AUTOMATIC, (String) null);  // Should be ignored.

		// Should be ignored.
		// TODO this should be filtered.
//		this.testDb.addTestTrack(BigInteger.ZERO, BigInteger.valueOf(0x1234567123L));  // No tags.
		this.testDb.addTestTrack(BigInteger.ZERO, null);  // No SHA1.

		this.req.requestURI = "/mlists/LOCALMMDB/server-test-db.local.db3/sha1tags";
		this.undertest.service(this.req, this.resp);

		assertEquals("[{\"sha1\":\"1234567890abcdef\",\"tags\":[\"bar\",\"foo\"]}]", this.resp.getOutputAsString());
		assertEquals("text/json;charset=utf-8", this.resp.contentType);
	}

}
