package com.vaguehope.morrigan.server;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;

import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.MediaFactory;
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

	private TestMixedMediaDb testDb;
	private PlayerReader playerReader;
	private MediaFactory mediaFactory;
	private ExecutorService executor;
	private AsyncTasksRegister asyncTasksRegister;
	private AsyncActions asyncActions;
	private Transcoder transcoder;

	private MlistsServlet undertest;

	private MockHttpServletRequest req;
	private MockHttpServletResponse resp;

	@Before
	public void before () throws Exception {
		this.testDb = new TestMixedMediaDb();
		this.playerReader = new MockPlayerReader();
		this.mediaFactory = new MediaFactoryImpl(null);
		this.executor = Executors.newCachedThreadPool(new DaemonThreadFactory("test"));
		this.asyncTasksRegister = new AsyncTasksRegisterImpl(this.executor);
		this.asyncActions = new AsyncActions(this.asyncTasksRegister, this.mediaFactory);
		this.transcoder = new Transcoder("test");
		this.undertest = new MlistsServlet(this.playerReader, this.mediaFactory, this.asyncActions, this.transcoder);
		this.req = new MockHttpServletRequest();
		this.resp = new MockHttpServletResponse();
	}

	@Test
	public void itServesRootList () throws Exception {
		this.req.requestURI = "/mlists";
		this.undertest.doGet(this.req, this.resp);

		final String expected = IoHelper.readAsString(getClass().getResourceAsStream("/mlists.xml"));
		assertEquals(expected, this.resp.getOutputAsString());
	}

	@Test
	public void itDoesNotIncludeDeletedTagsIfNotRequested () throws Exception {
		final IMixedMediaItem item = this.testDb.addTestTrack();
		// TODO
	}

	@Test
	public void itIncludesDeletedTagsIfRequested () throws Exception {
		// TODO!
	}

}
