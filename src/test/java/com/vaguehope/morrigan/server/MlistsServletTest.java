package com.vaguehope.morrigan.server;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.common.servlet.MockHttpServletRequest;
import com.vaguehope.common.servlet.MockHttpServletResponse;
import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaItem.MediaType;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.internal.MediaFactoryImpl;
import com.vaguehope.morrigan.model.media.test.TestMediaDb;
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

	private TestMediaDb testDb;
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

		this.testDb = new TestMediaDb("server-test-db");
		this.mediaFactory.addLocalMixedMediaDb(this.testDb);
	}

	@SuppressWarnings("resource")
	@Test
	public void itServesRootList () throws Exception {
		this.req.setRequestURI("/mlists");
		this.undertest.service(this.req, this.resp);

		final String expected = IoHelper.readAsString(getClass().getResourceAsStream("/mlists.xml"));
		assertEquals(expected, this.resp.getOutputAsString());
	}

	@Test
	public void itServesSavedViewsNoFile () throws Exception {
		this.req.setRequestURI("/mlists/savedviews");
		this.undertest.service(this.req, this.resp);
		assertEquals("[]", this.resp.getOutputAsString());
	}

	@SuppressWarnings("resource")
	@Test
	public void itServesSavedViewsFile () throws Exception {
		this.req.setRequestURI("/mlists/savedviews");

		final String expected = IoHelper.readAsString(getClass().getResourceAsStream("/savedviews.json"));
		IoHelper.write(expected, this.tmp.newFile("savedviews.json"));

		this.undertest.service(this.req, this.resp);
		assertEquals(expected, this.resp.getOutputAsString());
		assertEquals("text/json;charset=utf-8", this.resp.getContentType());
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
		final MediaItem t0 = this.testDb.addTestTrack(BigInteger.ZERO, BigInteger.valueOf(0x1234567890abcdefL));
		this.testDb.addTag(t0, "foo", MediaTagType.MANUAL, null, new Date(123456789000L), false);
		this.testDb.addTag(t0, "bar", MediaTagType.MANUAL, null, new Date(987654321000L), false);
		this.testDb.addTag(t0, "no-mod-date", MediaTagType.MANUAL, null, null, false);  // Because apparently this is still a thing.
		this.testDb.addTag(t0, "gone", MediaTagType.MANUAL, null, new Date(155666777000L), true);
		this.testDb.addTag(t0, "bat", MediaTagType.AUTOMATIC, "some class", new Date(155666888000L), false);

		final MediaItem t1 = this.testDb.addTestTrack(BigInteger.ZERO, BigInteger.valueOf(0x1114567890abcdefL));
		this.testDb.setItemMediaType(t1, MediaType.PICTURE);
		this.testDb.addTag(t1, "pic", MediaTagType.MANUAL, null, new Date(155666888000L), false);

		// Should be ignored.
		// TODO this should be filtered.
//		this.testDb.addTestTrack(BigInteger.ZERO, BigInteger.valueOf(0x1234567123L));  // No tags.
		this.testDb.addTestTrack(BigInteger.ZERO, null);  // No SHA1.

		// The force read will query DB, applying DefaultMediaType to getMediaItems().
		this.testDb.forceRead();

		this.req.setRequestURI("/mlists/LOCAL:l=server-test-db/sha1tags");
		for (final boolean includeautotags : Arrays.asList(false, true)) {  // TODO this should be 2 tests but its late.
			this.req.setParameter("includeautotags", includeautotags ? "true" : "");
			this.resp = new MockHttpServletResponse();  // reset between runs.
			this.undertest.service(this.req, this.resp);

			final String expected = "["
					+ "{\"sha1\":\"1234567890abcdef\",\"tags\":["
					+ "{\"tag\":\"bar\",\"mod\":987654321000,\"del\":false},"
					+ "{\"tag\":\"foo\",\"mod\":123456789000,\"del\":false},"
					+ "{\"tag\":\"gone\",\"mod\":155666777000,\"del\":true},"
					+ "{\"tag\":\"no-mod-date\",\"del\":false}"
					+ (includeautotags ? ",{\"tag\":\"bat\",\"cls\":\"some class\",\"mod\":155666888000,\"del\":false}" : "")
					+ "]},"
					+ "{\"sha1\":\"1114567890abcdef\",\"tags\":["
					+ "{\"tag\":\"pic\",\"mod\":155666888000,\"del\":false}"
					+ "]}]";
			assertEquals(expected, this.resp.getOutputAsString());
			assertEquals("text/json;charset=utf-8", this.resp.getContentType());
		}
	}

}
