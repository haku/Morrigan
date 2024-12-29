package com.vaguehope.morrigan.model.media.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMediaItemDb;
import com.vaguehope.morrigan.model.media.MediaListReference;

public class MediaFactoryImplTest {

	private static final Random RND = new Random(System.currentTimeMillis());
	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	private MediaFactoryImpl undertest;

	@Before
	public void before() throws Exception {
		final Config config = new Config(this.tmp.getRoot());
		this.undertest = new MediaFactoryImpl(config, null);
	}

	@Test
	public void itGetsLocalByMid() throws Exception {
		final IMediaItemDb db = this.undertest.createLocalMixedMediaDb("test" + RND.nextLong());
		final MediaListReference ref = this.undertest.getAllLocalMixedMediaDbs().iterator().next();
		assertEquals(db, this.undertest.getMediaListByMid(ref.getMid(), null));
		assertEquals(db, this.undertest.getMediaListByRef(ref));

		final String filter = "some-filter" + RND.nextLong();
		final IMediaItemDb dbWithFilter = this.undertest.getLocalMixedMediaDb(db.getDbPath(), filter);
		assertEquals(dbWithFilter, this.undertest.getMediaListByMid(ref.getMid(), filter));
		assertEquals(dbWithFilter, this.undertest.getMediaListByRef(ref, filter));
	}

	@Test
	public void itGetsRemoteByMid() throws Exception {
		final IMediaItemDb external = mock(IMediaItemDb.class);
		final String id = "id" + RND.nextLong();
		when(external.getListId()).thenReturn(id);
		when(external.getListName()).thenReturn("name for " + id);
		this.undertest.addExternalList(external);

		final MediaListReference ref = this.undertest.getExternalList().iterator().next();
		assertEquals(external, this.undertest.getMediaListByMid(ref.getMid(), null));
		assertEquals(external, this.undertest.getMediaListByRef(ref));
	}

}
